package com.prometis.appbuilder.workflow;

import com.prometis.appbuilder.dto.RequestMessage;
import com.prometis.appbuilder.dto.ResponseMessage;
import com.prometis.appbuilder.dto.ResultType;
import com.prometis.appbuilder.executor.script.ScriptEngine;
import com.prometis.appbuilder.node.*;
import com.prometis.appbuilder.node.code.FunctionType;
import com.prometis.appbuilder.security.ip.IpAccessValidator;
import com.prometis.appbuilder.security.service.AuthService;
import com.prometis.appbuilder.workflow.code.BranchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 조건분기 뒤에 에러메시지 노드를 이어두고, 조건에 걸리면 설정한 에러응답으로 흐름이 끝나는지 확인한다.
 */
class WorkflowErrorMessageNodeTest {
    private static final long WORKFLOW_ID = 1L;
    private static final String WORKFLOW_CODE = "TEST_ERROR_NODE";

    private WorkflowNodeRepository workflowNodeRepository;
    private WorkflowEdgeRepository workflowEdgeRepository;
    private WorkflowConditionRepository workflowConditionRepository;
    private WorkflowErrorResponseRepository workflowErrorResponseRepository;
    private NodeExecutor sqlExecutor;
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        WorkflowRepository workflowRepository = mock(WorkflowRepository.class);
        workflowNodeRepository = mock(WorkflowNodeRepository.class);
        workflowEdgeRepository = mock(WorkflowEdgeRepository.class);
        workflowConditionRepository = mock(WorkflowConditionRepository.class);
        workflowErrorResponseRepository = mock(WorkflowErrorResponseRepository.class);
        NodeExecutorFactory nodeExecutorFactory = mock(NodeExecutorFactory.class);
        sqlExecutor = mock(NodeExecutor.class);

        Workflow workflow = Workflow.builder()
                .id(WORKFLOW_ID)
                .workflowCode(WORKFLOW_CODE)
                .displayName("에러메시지 노드")
                .useAuthValidation(false)
                .build();

        when(workflowRepository.findByWorkflowCode(WORKFLOW_CODE)).thenReturn(Optional.of(workflow));
        when(nodeExecutorFactory.instance(FunctionType.SQL)).thenReturn(sqlExecutor);
        when(sqlExecutor.execute(any(), anyString(), any(), anyList())).thenAnswer(invocation -> NodeResult.builder()
                .resultType(com.prometis.appbuilder.node.code.ResultType.SUCCESS)
                .results(List.of(Map.of("step", invocation.getArgument(1))))
                .build());

        workflowService = new WorkflowService(
                workflowRepository,
                workflowNodeRepository,
                workflowEdgeRepository,
                workflowConditionRepository,
                workflowErrorResponseRepository,
                mock(WorkflowAuthorityRepository.class),
                nodeExecutorFactory,
                new ConditionEvaluator(new ScriptEngine()),
                mock(AuthService.class),
                mock(IpAccessValidator.class)
        );

        // n1(조건분기) -- if grade === 'F' --> n2(에러메시지) --> n4(SQL, 에러 뒤에 이어져 있어도 실행되면 안 된다)
        //              -- else -------------> n3(SQL)
        when(workflowNodeRepository.findByWorkflowId(WORKFLOW_ID)).thenReturn(List.of(
                node("n1", FunctionType.CONDITION, 1),
                node("n2", FunctionType.ERROR_MESSAGE, 2),
                node("n3", FunctionType.SQL, 3),
                node("n4", FunctionType.SQL, 4)
        ));
        when(workflowEdgeRepository.findByWorkflowId(WORKFLOW_ID)).thenReturn(List.of(
                edge("n1", "n2", BranchType.CASE, "c1", 0),
                edge("n1", "n3", BranchType.ELSE, null, 1),
                edge("n2", "n4", BranchType.DEFAULT, null, 2)
        ));
        when(workflowConditionRepository.findByWorkflowId(WORKFLOW_ID)).thenReturn(List.of(
                WorkflowCondition.builder()
                        .workflowId(WORKFLOW_ID)
                        .nodeId("n1")
                        .branchId("c1")
                        .conditionExpression("params[0].grade === 'F'")
                        .orderNum(0)
                        .build()
        ));
    }

    private WorkflowNode node(String nodeId, FunctionType functionType, int orderNum) {
        return WorkflowNode.builder()
                .id((long) orderNum)
                .workflowId(WORKFLOW_ID)
                .nodeId(nodeId)
                .functionName(nodeId)
                .functionType(functionType)
                .orderNum(orderNum)
                .requestMessageId("IN")
                .responseMessageId("OUT_" + nodeId)
                .build();
    }

    private WorkflowEdge edge(String source, String target, BranchType branchType, String branchId, int orderNum) {
        return WorkflowEdge.builder()
                .workflowId(WORKFLOW_ID)
                .sourceNodeId(source)
                .targetNodeId(target)
                .branchType(branchType)
                .branchId(branchId)
                .orderNum(orderNum)
                .build();
    }

    private WorkflowErrorResponse errorResponse(Integer status, String contents) {
        return WorkflowErrorResponse.builder()
                .workflowId(WORKFLOW_ID)
                .nodeId("n2")
                .status(status)
                .code("E-EVAL-001")
                .message("F 등급은 저장할 수 없습니다.")
                .contents(contents)
                .build();
    }

    private ResponseMessage execute(String grade) {
        Map<String, List<Map<String, Object>>> body = new HashMap<>();
        body.put("IN", new ArrayList<>(List.of(new HashMap<>(Map.of("grade", grade)))));

        RequestMessage.Header header = new RequestMessage.Header();
        header.setWorkflowCode(WORKFLOW_CODE);

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setHeader(header);
        requestMessage.setBody(body);

        return workflowService.execute(null, null, requestMessage);
    }

    @Test
    public void 조건에_걸리면_설정한_에러응답으로_끝나고_뒤의_노드는_실행하지_않는다() {
        when(workflowErrorResponseRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(List.of(errorResponse(422, "IN, 없는메시지")));

        ResponseMessage response = execute("F");

        assertEquals(ResultType.ERROR, response.getResultType());
        assertEquals(422, response.getStatus());
        assertEquals("E-EVAL-001", response.getCode());
        assertEquals("F 등급은 저장할 수 없습니다.", response.getMessage());

        // contents 에 적은 메시지ID 만 담긴다. 채워진 적 없는 메시지ID 는 빈 목록으로 온다.
        assertEquals(List.of("IN", "없는메시지"), new ArrayList<>(response.getContents().keySet()));
        assertEquals("F", response.getContents().get("IN").get(0).get("grade"));
        assertTrue(response.getContents().get("없는메시지").isEmpty());

        verify(sqlExecutor, never()).execute(any(), anyString(), any(), anyList());
    }

    @Test
    public void 조건에_걸리지_않으면_다른_가지로_정상_실행된다() {
        when(workflowErrorResponseRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(List.of(errorResponse(422, "")));

        ResponseMessage response = execute("A");

        assertEquals(ResultType.SUCCESS, response.getResultType());
        assertEquals(Set.of("OUT_n3"), response.getContents().keySet());
        verify(sqlExecutor).execute(any(), eq("n3"), any(), anyList());
        verify(sqlExecutor, never()).execute(any(), eq("n4"), any(), anyList());
    }

    @Test
    public void contents를_비워두면_빈_contents로_응답한다() {
        when(workflowErrorResponseRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(List.of(errorResponse(400, null)));

        ResponseMessage response = execute("F");

        assertEquals(400, response.getStatus());
        assertTrue(response.getContents().isEmpty());
    }

    @Test
    public void 에러메시지_노드_설정이_없으면_설정누락_에러로_알린다() {
        when(workflowErrorResponseRepository.findByWorkflowId(WORKFLOW_ID)).thenReturn(List.of());

        ResponseMessage response = execute("F");

        assertEquals(WorkflowErrorMessage.NOT_SETTING_ERROR_RESPONSE.getCode(), response.getCode());
        assertEquals(500, response.getStatus());
    }

    @Test
    public void status는_HTTP_에러상태가_아니면_400으로_보낸다() {
        assertEquals(400, WorkflowErrorResponse.resolveStatus(null));
        assertEquals(400, WorkflowErrorResponse.resolveStatus(200));
        assertEquals(400, WorkflowErrorResponse.resolveStatus(302));
        assertEquals(400, WorkflowErrorResponse.resolveStatus(499));
        assertEquals(400, WorkflowErrorResponse.resolveStatus(9999));
        assertEquals(404, WorkflowErrorResponse.resolveStatus(404));
        assertEquals(503, WorkflowErrorResponse.resolveStatus(503));
    }
}
