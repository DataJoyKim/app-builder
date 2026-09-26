package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.workflow.*;
import com.prometis.appbuilder.console.rest.WorkflowRestController;
import com.prometis.appbuilder.app.node.WorkflowNode;
import com.prometis.appbuilder.app.node.WorkflowNodeRepository;
import com.prometis.appbuilder.app.workflow.code.BranchType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 워크플로우 빌더가 저장 버튼을 눌렀을 때 보내는 것과 같은 모양의 요청으로 저장 경로 전체를 확인한다.
 * 개발용 H2 파일은 앱이 잡고 있을 수 있어, 엔티티로 스키마를 새로 만드는 메모리 DB에서 돌린다.
 */
@DataJpaTest
@Import(WorkflowRestController.class)
class WorkflowSaveIntegrationTest {
    @Autowired
    private WorkflowRestController workflowRestController;
    @Autowired
    private WorkflowRepository workflowRepository;
    @Autowired
    private WorkflowNodeRepository workflowNodeRepository;
    @Autowired
    private WorkflowEdgeRepository workflowEdgeRepository;
    @Autowired
    private WorkflowConditionRepository workflowConditionRepository;
    @Autowired
    private WorkflowErrorResponseRepository workflowErrorResponseRepository;

    private Map<String,Object> node(String nodeId, String functionType, String functionName, int orderNum) {
        Map<String,Object> node = new HashMap<>();
        node.put("nodeId", nodeId);
        node.put("functionName", functionName);
        node.put("functionType", functionType);
        node.put("orderNum", orderNum);
        node.put("isLogging", false);
        node.put("requestMessageId", "IN");
        node.put("responseMessageId", "OUT_" + nodeId);
        // 빌더 캔버스에서의 위치. 실행에는 쓰이지 않지만 화면을 다시 열 때 그대로 살아나야 한다.
        node.put("positionX", orderNum * 100);
        node.put("positionY", orderNum * 200);
        return node;
    }

    private Map<String,Object> edge(String source, String target, String branchType, String branchId, int orderNum) {
        Map<String,Object> edge = new HashMap<>();
        edge.put("sourceNodeId", source);
        edge.put("targetNodeId", target);
        edge.put("branchType", branchType);
        edge.put("branchId", branchId);
        edge.put("orderNum", orderNum);
        return edge;
    }

    private Map<String,Object> condition(String nodeId, String branchId, String expression, int orderNum) {
        Map<String,Object> condition = new HashMap<>();
        condition.put("nodeId", nodeId);
        condition.put("branchId", branchId);
        condition.put("conditionExpression", expression);
        condition.put("orderNum", orderNum);
        return condition;
    }

    @Test
    public void 조건분기가_있는_워크플로우가_저장되고_그대로_다시_읽힌다() {
        Map<String,Object> workflow = new HashMap<>();
        workflow.put("id", "");
        workflow.put("workflowCode", "TEST1000_R01");
        workflow.put("displayName", "조건분기 저장 확인");
        workflow.put("note", "");
        workflow.put("useAuthValidation", false);

        Map<String,Object> params = new HashMap<>();
        params.put("workflow", workflow);
        params.put("workflowNodes", List.of(
                node("node-1", "SQL", "EVAL0001_R01", 1),
                node("node-2", "CONDITION", "", 2),
                node("node-3", "SQL", "IF_STEP", 3),
                node("node-4", "SQL", "ELSE_IF_STEP", 4),
                node("node-5", "SQL", "ELSE_STEP", 5)
        ));
        params.put("workflowEdges", List.of(
                edge("node-1", "node-2", "DEFAULT", null, 0),
                edge("node-2", "node-3", "CASE", "c1", 1),
                edge("node-2", "node-4", "CASE", "c2", 2),
                edge("node-2", "node-5", "ELSE", null, 3)
        ));
        params.put("workflowConditions", List.of(
                condition("node-2", "c1", "params[0].eval_way_cd === 'QUANT_EVAL'", 0),
                condition("node-2", "c2", "params[0].eval_way_cd === 'QUAL_EVAL'", 1)
        ));
        params.put("workflowAuthority", new ArrayList<Map<String,Object>>());

        workflowRestController.save(params);

        Workflow saved = workflowRepository.findByWorkflowCode("TEST1000_R01").orElseThrow();

        List<WorkflowNode> functions = workflowNodeRepository.findByWorkflowId(saved.getId());
        List<WorkflowEdge> edges = workflowEdgeRepository.findByWorkflowIdOrderByOrderNum(saved.getId());
        List<WorkflowCondition> conditions = workflowConditionRepository.findByWorkflowIdOrderByOrderNum(saved.getId());

        assertEquals(5, functions.size(), "노드");
        assertEquals(4, edges.size(), "연결선");
        assertEquals(2, conditions.size(), "조건");

        // 조건분기 노드는 따로 만들어둔 기능이 없어 노드 식별자를 기능명으로 대신 채운다.
        WorkflowNode conditionNode = functions.stream()
                .filter(WorkflowNode::isCondition)
                .findFirst()
                .orElseThrow();
        assertEquals("node-2", conditionNode.getFunctionName());

        assertEquals(BranchType.CASE, edges.get(1).getBranchType());
        assertEquals("c1", edges.get(1).getBranchId());
        assertEquals(BranchType.ELSE, edges.get(3).getBranchType());

        // 저장한 것이 실행 가능한 그래프로 그대로 돌아오는지까지 확인한다.
        WorkflowGraph graph = WorkflowGraph.of(functions, edges, conditions);

        WorkflowNode start = graph.getStartNode();
        assertEquals("node-1", start.getNodeId());

        WorkflowNode branch = graph.next(start, BranchType.DEFAULT);
        assertEquals("node-2", branch.getNodeId());

        assertEquals(2, graph.conditionsOf(branch).size());
        assertEquals("c1", graph.conditionsOf(branch).get(0).getBranchId());
        assertEquals("node-3", graph.nextCase(branch, "c1").getNodeId());
        assertEquals("node-4", graph.nextCase(branch, "c2").getNodeId());
        assertEquals("node-5", graph.nextElse(branch).getNodeId());
    }

    @Test
    public void 갈라진_가지가_같은_노드로_합쳐지게_저장된다() {
        Map<String,Object> workflow = new HashMap<>();
        workflow.put("id", "");
        workflow.put("workflowCode", "TEST1000_R03");
        workflow.put("displayName", "분기 합류 저장 확인");
        workflow.put("note", "");
        workflow.put("useAuthValidation", false);

        // if -> A -> C, else -> B -> C. 빌더에서 C 를 하나만 두고 양쪽에서 이어놓은 모양이다.
        Map<String,Object> params = new HashMap<>();
        params.put("workflow", workflow);
        params.put("workflowNodes", List.of(
                node("node-1", "CONDITION", "", 1),
                node("node-2", "SQL", "A_STEP", 2),
                node("node-3", "SQL", "B_STEP", 3),
                node("node-4", "SQL", "C_STEP", 4)
        ));
        params.put("workflowEdges", List.of(
                edge("node-1", "node-2", "CASE", "c1", 0),
                edge("node-1", "node-3", "ELSE", null, 1),
                edge("node-2", "node-4", "DEFAULT", null, 2),
                edge("node-3", "node-4", "DEFAULT", null, 3)
        ));
        params.put("workflowConditions", List.of(condition("node-1", "c1", "params[0].grade === 'A'", 0)));
        params.put("workflowAuthority", new ArrayList<Map<String,Object>>());

        workflowRestController.save(params);

        Workflow saved = workflowRepository.findByWorkflowCode("TEST1000_R03").orElseThrow();

        List<WorkflowNode> functions = workflowNodeRepository.findByWorkflowId(saved.getId());
        List<WorkflowEdge> edges = workflowEdgeRepository.findByWorkflowIdOrderByOrderNum(saved.getId());
        List<WorkflowCondition> conditions = workflowConditionRepository.findByWorkflowIdOrderByOrderNum(saved.getId());

        assertEquals(4, functions.size(), "노드");
        assertEquals(4, edges.size(), "연결선");

        WorkflowGraph graph = WorkflowGraph.of(functions, edges, conditions);

        WorkflowNode branch = graph.getStartNode();
        assertEquals("node-1", branch.getNodeId());

        WorkflowNode ifNode = graph.nextCase(branch, "c1");
        WorkflowNode elseNode = graph.nextElse(branch);
        assertEquals("node-2", ifNode.getNodeId());
        assertEquals("node-3", elseNode.getNodeId());

        // 어느 가지로 흘러도 같은 노드에 닿는다.
        assertEquals("node-4", graph.next(ifNode, BranchType.DEFAULT).getNodeId());
        assertEquals("node-4", graph.next(elseNode, BranchType.DEFAULT).getNodeId());

        // 캔버스 위치도 그대로 돌아온다.
        WorkflowNode cNode = functions.stream()
                .filter(f -> "node-4".equals(f.getNodeId()))
                .findFirst()
                .orElseThrow();
        assertEquals(400, cNode.getPositionX());
        assertEquals(800, cNode.getPositionY());
    }

    @Test
    public void 에러메시지_노드의_설정이_저장되고_그래프로_다시_읽힌다() {
        Map<String,Object> workflow = new HashMap<>();
        workflow.put("id", "");
        workflow.put("workflowCode", "TEST1000_R04");
        workflow.put("displayName", "에러메시지 저장 확인");
        workflow.put("note", "");
        workflow.put("useAuthValidation", false);

        Map<String,Object> errorResponse = new HashMap<>();
        errorResponse.put("nodeId", "node-2");
        errorResponse.put("status", 422);
        errorResponse.put("code", "E-EVAL-001");
        errorResponse.put("message", "F 등급은 저장할 수 없습니다.");
        errorResponse.put("contents", "IN, OUT_node-1");

        Map<String,Object> params = new HashMap<>();
        params.put("workflow", workflow);
        params.put("workflowNodes", List.of(
                node("node-1", "CONDITION", "", 1),
                node("node-2", "ERROR_MESSAGE", "", 2),
                node("node-3", "SQL", "SAVE_STEP", 3)
        ));
        params.put("workflowEdges", List.of(
                edge("node-1", "node-2", "CASE", "c1", 0),
                edge("node-1", "node-3", "ELSE", null, 1)
        ));
        params.put("workflowConditions", List.of(condition("node-1", "c1", "params[0].grade === 'F'", 0)));
        params.put("workflowErrorResponses", List.of(errorResponse));
        params.put("workflowAuthority", new ArrayList<Map<String,Object>>());

        workflowRestController.save(params);

        Workflow saved = workflowRepository.findByWorkflowCode("TEST1000_R04").orElseThrow();

        List<WorkflowNode> functions = workflowNodeRepository.findByWorkflowId(saved.getId());
        List<WorkflowErrorResponse> errorResponses = workflowErrorResponseRepository.findByWorkflowId(saved.getId());

        assertEquals(1, errorResponses.size(), "에러응답");

        // 에러메시지 노드도 따로 만들어둔 기능이 없어 노드 식별자를 기능명으로 대신 채운다.
        WorkflowNode errorNode = functions.stream()
                .filter(WorkflowNode::isErrorMessage)
                .findFirst()
                .orElseThrow();
        assertEquals("node-2", errorNode.getFunctionName());

        WorkflowGraph graph = WorkflowGraph.of(
                functions,
                workflowEdgeRepository.findByWorkflowIdOrderByOrderNum(saved.getId()),
                workflowConditionRepository.findByWorkflowIdOrderByOrderNum(saved.getId()),
                errorResponses
        );

        WorkflowErrorResponse loaded = graph.errorResponseOf(graph.nextCase(graph.getStartNode(), "c1"));
        assertEquals(422, loaded.getStatus());
        assertEquals("E-EVAL-001", loaded.getCode());
        assertEquals("F 등급은 저장할 수 없습니다.", loaded.getMessage());
        assertEquals("IN, OUT_node-1", loaded.getContents());

        // 다시 저장할 때 에러메시지 노드를 빼면 설정도 남지 않는다.
        workflow.put("id", String.valueOf(saved.getId()));
        params.put("workflowNodes", List.of(node("node-1", "SQL", "A", 1)));
        params.put("workflowEdges", new ArrayList<Map<String,Object>>());
        params.put("workflowConditions", new ArrayList<Map<String,Object>>());
        params.put("workflowErrorResponses", new ArrayList<Map<String,Object>>());

        workflowRestController.save(params);

        assertTrue(workflowErrorResponseRepository.findByWorkflowId(saved.getId()).isEmpty(), "에러응답");
    }

    @Test
    public void 다시_저장하면_이전_조건과_연결선은_남지_않는다() {
        Map<String,Object> workflow = new HashMap<>();
        workflow.put("id", "");
        workflow.put("workflowCode", "TEST1000_R02");
        workflow.put("displayName", "재저장 확인");
        workflow.put("note", "");
        workflow.put("useAuthValidation", false);

        Map<String,Object> params = new HashMap<>();
        params.put("workflow", workflow);
        params.put("workflowNodes", List.of(node("node-1", "CONDITION", "", 1), node("node-2", "SQL", "A", 2)));
        params.put("workflowEdges", List.of(edge("node-1", "node-2", "CASE", "c1", 0)));
        params.put("workflowConditions", List.of(condition("node-1", "c1", "1 === 1", 0)));
        params.put("workflowAuthority", new ArrayList<Map<String,Object>>());

        workflowRestController.save(params);

        Workflow saved = workflowRepository.findByWorkflowCode("TEST1000_R02").orElseThrow();

        // 같은 워크플로우를 조건 없는 노드 하나로 다시 저장한다.
        workflow.put("id", String.valueOf(saved.getId()));
        params.put("workflowNodes", List.of(node("node-1", "SQL", "A", 1)));
        params.put("workflowEdges", new ArrayList<Map<String,Object>>());
        params.put("workflowConditions", new ArrayList<Map<String,Object>>());

        workflowRestController.save(params);

        assertEquals(1, workflowNodeRepository.findByWorkflowId(saved.getId()).size(), "노드");
        assertTrue(workflowEdgeRepository.findByWorkflowId(saved.getId()).isEmpty(), "연결선");
        assertTrue(workflowConditionRepository.findByWorkflowId(saved.getId()).isEmpty(), "조건");
    }
}
