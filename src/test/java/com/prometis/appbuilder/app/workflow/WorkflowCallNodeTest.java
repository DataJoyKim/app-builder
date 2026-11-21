package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.dto.ResponseMessage;
import com.prometis.appbuilder.app.dto.ResultType;
import com.prometis.appbuilder.app.executor.script.ScriptEngine;
import com.prometis.appbuilder.app.node.*;
import com.prometis.appbuilder.app.node.code.FunctionType;
import com.prometis.appbuilder.app.workflow.code.BranchType;
import com.prometis.appbuilder.platform.application.ApplicationGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 워크플로우 실행(WORKFLOW) 노드가 다른 워크플로우를 이어서 실행하고, 순환 호출은 실행 중에도 막히는지 확인한다.
 */
class WorkflowCallNodeTest {
    private static final String APPLICATION_ID = "ehr";

    private WorkflowRepository workflowRepository;
    private WorkflowNodeRepository workflowNodeRepository;
    private WorkflowEdgeRepository workflowEdgeRepository;
    private WorkflowErrorResponseRepository workflowErrorResponseRepository;
    private WorkflowGuard workflowGuard;
    private NodeExecutor sqlExecutor;
    private WorkflowService workflowService;

    private final Map<Long, List<WorkflowNode>> nodesByWorkflow = new HashMap<>();
    private final Map<Long, List<WorkflowEdge>> edgesByWorkflow = new HashMap<>();

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        workflowNodeRepository = mock(WorkflowNodeRepository.class);
        workflowEdgeRepository = mock(WorkflowEdgeRepository.class);
        WorkflowConditionRepository workflowConditionRepository = mock(WorkflowConditionRepository.class);
        workflowErrorResponseRepository = mock(WorkflowErrorResponseRepository.class);
        NodeExecutorFactory nodeExecutorFactory = mock(NodeExecutorFactory.class);
        workflowGuard = mock(WorkflowGuard.class);
        sqlExecutor = mock(NodeExecutor.class);

        when(nodeExecutorFactory.instance(FunctionType.SQL)).thenReturn(sqlExecutor);
        // SQL 노드는 받은 첫 행에 자기 기능명을 step 으로 덧붙여 돌려준다
        when(sqlExecutor.execute(any(), anyString(), any(), anyList())).thenAnswer(invocation -> {
            List<Map<String, Object>> params = invocation.getArgument(3);
            Map<String, Object> row = new HashMap<>(params.isEmpty() ? Map.of() : params.get(0));
            row.put("step", invocation.getArgument(1));
            return NodeResult.builder().resultType(com.prometis.appbuilder.app.node.code.ResultType.SUCCESS).results(List.of(row)).build();
        });

        when(workflowNodeRepository.findByWorkflowId(anyLong())).thenAnswer(i -> nodesByWorkflow.getOrDefault((Long) i.getArgument(0), List.of()));
        when(workflowEdgeRepository.findByWorkflowId(anyLong())).thenAnswer(i -> edgesByWorkflow.getOrDefault((Long) i.getArgument(0), List.of()));
        when(workflowConditionRepository.findByWorkflowId(anyLong())).thenReturn(List.of());
        when(workflowErrorResponseRepository.findByWorkflowId(anyLong())).thenReturn(List.of());

        workflowService = new WorkflowService(
                workflowRepository,
                workflowNodeRepository,
                workflowEdgeRepository,
                workflowConditionRepository,
                workflowErrorResponseRepository,
                nodeExecutorFactory,
                new ConditionEvaluator(new ScriptEngine()),
                workflowGuard,
                mock(ApplicationGuard.class),
                WorkflowProperties.defaults()
        );
    }

    private void workflow(long id, String code, List<WorkflowNode> nodes) {
        Workflow workflow = Workflow.builder()
                .applicationId(APPLICATION_ID)
                .id(id)
                .workflowCode(code)
                .displayName(code)
                .useAuthValidation(false)
                .build();
        when(workflowRepository.findByApplicationIdAndWorkflowCode(APPLICATION_ID, code)).thenReturn(Optional.of(workflow));

        nodesByWorkflow.put(id, nodes);

        // 노드를 적은 순서대로 잇는다
        List<WorkflowEdge> edges = new ArrayList<>();
        for(int i = 1; i < nodes.size(); i++) {
            edges.add(WorkflowEdge.builder()
                    .workflowId(id)
                    .sourceNodeId(nodes.get(i - 1).getNodeId())
                    .targetNodeId(nodes.get(i).getNodeId())
                    .branchType(BranchType.DEFAULT)
                    .orderNum(i)
                    .build());
        }
        edgesByWorkflow.put(id, edges);
    }

    private static WorkflowNode node(long workflowId, String nodeId, FunctionType type, String functionName, String in, String out, int orderNum) {
        return WorkflowNode.builder()
                .id(workflowId * 100 + orderNum)
                .workflowId(workflowId)
                .nodeId(nodeId)
                .functionName(functionName)
                .functionType(type)
                .orderNum(orderNum)
                .requestMessageId(in)
                .responseMessageId(out)
                .build();
    }

    private ResponseMessage execute(String code, Map<String, Object> input) {
        Map<String, List<Map<String, Object>>> body = new HashMap<>();
        body.put("IN", new ArrayList<>(List.of(new HashMap<>(input))));

        RequestMessage.Header header = new RequestMessage.Header();
        header.setWorkflowCode(code);

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setHeader(header);
        requestMessage.setBody(body);

        return workflowService.execute(null, null, APPLICATION_ID, requestMessage);
    }

    @Test
    public void 실행한_워크플로우의_응답을_받아_다음_노드로_이어간다() throws Exception {
        // PARENT: p1(SQL IN->P_OUT1) -> p2(WORKFLOW CHILD) -> p3(SQL C_OUT->P_OUT3)
        // CHILD : c1(SQL P_OUT1->C_OUT)
        workflow(1L, "PARENT", List.of(
                node(1L, "p1", FunctionType.SQL, "P1", "IN", "P_OUT1", 1),
                node(1L, "p2", FunctionType.WORKFLOW, "CHILD", null, null, 2),
                node(1L, "p3", FunctionType.SQL, "P3", "C_OUT", "P_OUT3", 3)
        ));
        workflow(2L, "CHILD", List.of(
                node(2L, "c1", FunctionType.SQL, "C1", "P_OUT1", "C_OUT", 1)
        ));

        ResponseMessage response = execute("PARENT", Map.of("userId", "u1"));

        assertEquals(ResultType.SUCCESS, response.getResultType());
        // 하위 워크플로우는 부모가 채운 메시지(P_OUT1)를 그대로 읽는다
        assertEquals("u1", response.getContents().get("C_OUT").get(0).get("userId"));
        assertEquals("C1", response.getContents().get("C_OUT").get(0).get("step"));
        // 하위 워크플로우의 응답(C_OUT)을 부모의 다음 노드가 이어서 읽는다
        assertEquals("P3", response.getContents().get("P_OUT3").get(0).get("step"));
        assertEquals("u1", response.getContents().get("P_OUT3").get(0).get("userId"));
        assertEquals(Set.of("P_OUT1", "C_OUT", "P_OUT3"), response.getContents().keySet());

        // 인증/권한/IP/도메인 검사는 처음 호출된 워크플로우에서 한 번만 한다
        verify(workflowGuard, times(1)).check(any(), eq(APPLICATION_ID), any());
    }

    @Test
    public void 실행한_워크플로우가_에러메시지로_끝나면_그_에러로_끝내고_뒤의_노드는_실행하지_않는다() {
        workflow(1L, "PARENT", List.of(
                node(1L, "p1", FunctionType.WORKFLOW, "CHILD", null, null, 1),
                node(1L, "p2", FunctionType.SQL, "P2", "IN", "P_OUT2", 2)
        ));
        workflow(2L, "CHILD", List.of(
                node(2L, "c1", FunctionType.ERROR_MESSAGE, "c1", null, null, 1)
        ));
        when(workflowErrorResponseRepository.findByWorkflowId(2L)).thenReturn(List.of(WorkflowErrorResponse.builder()
                .workflowId(2L).nodeId("c1").status(422).code("E-CHILD-001").message("하위 에러").build()));

        ResponseMessage response = execute("PARENT", Map.of());

        assertEquals(ResultType.ERROR, response.getResultType());
        assertEquals(422, response.getStatus());
        assertEquals("E-CHILD-001", response.getCode());
        verify(sqlExecutor, never()).execute(any(), eq("P2"), any(), anyList());
    }

    @Test
    public void 저장된_뒤에_순환이_생겨도_실행중에_막는다() {
        workflow(1L, "A", List.of(node(1L, "a1", FunctionType.WORKFLOW, "B", null, null, 1)));
        workflow(2L, "B", List.of(node(2L, "b1", FunctionType.WORKFLOW, "A", null, null, 1)));

        ResponseMessage response = execute("A", Map.of());

        assertEquals(WorkflowErrorMessage.CIRCULAR_WORKFLOW_CALL.getCode(), response.getCode());
    }

    @Test
    public void 호출_깊이가_한도를_넘으면_막는다() {
        int depth = WorkflowProperties.DEFAULT_MAX_CALL_DEPTH + 1;
        for(int i = 1; i <= depth; i++) {
            workflow(i, "W" + i, List.of(node(i, "n" + i, FunctionType.WORKFLOW, "W" + (i + 1), null, null, 1)));
        }

        ResponseMessage response = execute("W1", Map.of());

        assertEquals(WorkflowErrorMessage.EXCEED_MAX_CALL_DEPTH.getCode(), response.getCode());
    }

    @Test
    public void 없는_워크플로우를_가리키면_에러다() {
        workflow(1L, "A", List.of(node(1L, "a1", FunctionType.WORKFLOW, "NO_WF", null, null, 1)));
        when(workflowRepository.findByApplicationIdAndWorkflowCode(APPLICATION_ID, "NO_WF")).thenReturn(Optional.empty());

        ResponseMessage response = execute("A", Map.of());

        assertEquals(WorkflowErrorMessage.NOT_FOUND_CALL_WORKFLOW.getCode(), response.getCode());
    }
}
