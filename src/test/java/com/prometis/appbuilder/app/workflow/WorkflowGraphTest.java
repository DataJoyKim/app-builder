package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.executor.script.ScriptEngine;
import com.prometis.appbuilder.app.executor.script.ScriptEngineExecuteException;
import com.prometis.appbuilder.app.node.ConditionEvaluator;
import com.prometis.appbuilder.app.node.WorkflowNode;
import com.prometis.appbuilder.app.node.code.FunctionType;
import com.prometis.appbuilder.app.workflow.WorkflowCondition;
import com.prometis.appbuilder.app.workflow.WorkflowEdge;
import com.prometis.appbuilder.app.workflow.WorkflowGraph;
import com.prometis.appbuilder.app.workflow.code.BranchType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowGraphTest {

    private WorkflowNode node(String nodeId, FunctionType functionType, int orderNum) {
        return WorkflowNode.builder()
                .id((long) orderNum)
                .workflowId(1L)
                .nodeId(nodeId)
                .functionName(nodeId)
                .functionType(functionType)
                .orderNum(orderNum)
                .build();
    }

    private WorkflowEdge edge(String source, String target, BranchType branchType, String branchId, int orderNum) {
        return WorkflowEdge.builder()
                .workflowId(1L)
                .sourceNodeId(source)
                .targetNodeId(target)
                .branchType(branchType)
                .branchId(branchId)
                .orderNum(orderNum)
                .build();
    }

    private WorkflowCondition condition(String nodeId, String branchId, String expression, int orderNum) {
        return WorkflowCondition.builder()
                .workflowId(1L)
                .nodeId(nodeId)
                .branchId(branchId)
                .conditionExpression(expression)
                .orderNum(orderNum)
                .build();
    }

    @Test
    public void 연결선을_따라서만_흐른다() {
        WorkflowNode first = node("n1", FunctionType.SQL, 1);
        WorkflowNode second = node("n2", FunctionType.SQL, 2);

        // orderNum 은 빌더의 표시 순서일 뿐이라 연결선이 없으면 첫 노드에서 끝난다.
        WorkflowGraph noEdges = WorkflowGraph.of(List.of(second, first), List.of());

        assertEquals(first, noEdges.getStartNode());
        assertNull(noEdges.next(first, BranchType.DEFAULT));

        WorkflowGraph linked = WorkflowGraph.of(
                List.of(second, first),
                List.of(edge("n1", "n2", BranchType.DEFAULT, null, 0))
        );

        assertEquals(first, linked.getStartNode());
        assertEquals(second, linked.next(first, BranchType.DEFAULT));
        assertNull(linked.next(second, BranchType.DEFAULT));
    }

    @Test
    public void 조건은_판정순서대로_들어오고_각_가지는_branchId로_이어진다() {
        WorkflowNode start = node("n1", FunctionType.SQL, 1);
        WorkflowNode branch = node("n2", FunctionType.CONDITION, 2);
        WorkflowNode ifNode = node("n3", FunctionType.ENTITY, 3);
        WorkflowNode elseIfNode = node("n4", FunctionType.SQL, 4);
        WorkflowNode elseNode = node("n5", FunctionType.NOTIFICATION, 5);

        WorkflowGraph graph = WorkflowGraph.of(
                List.of(start, branch, ifNode, elseIfNode, elseNode),
                List.of(
                        edge("n1", "n2", BranchType.DEFAULT, null, 0),
                        edge("n2", "n3", BranchType.CASE, "c1", 1),
                        edge("n2", "n4", BranchType.CASE, "c2", 2),
                        edge("n2", "n5", BranchType.ELSE, null, 3)
                ),
                // 일부러 뒤섞어서 넣어도 orderNum 순서로 판정되어야 한다.
                List.of(
                        condition("n2", "c2", "params[0].grade === 'B'", 2),
                        condition("n2", "c1", "params[0].grade === 'A'", 1)
                )
        );

        assertEquals(start, graph.getStartNode());
        assertEquals(branch, graph.next(start, BranchType.DEFAULT));

        List<WorkflowCondition> conditions = graph.conditionsOf(branch);
        assertEquals(2, conditions.size());
        assertEquals("c1", conditions.get(0).getBranchId());
        assertEquals("c2", conditions.get(1).getBranchId());

        assertEquals(ifNode, graph.nextCase(branch, "c1"));
        assertEquals(elseIfNode, graph.nextCase(branch, "c2"));
        assertEquals(elseNode, graph.nextElse(branch));
        assertNull(graph.nextCase(branch, "없는가지"));
        assertNull(graph.next(branch, BranchType.DEFAULT));
    }

    @Test
    public void 갈라진_가지가_같은_노드로_다시_합쳐진다() {
        // if -> A -> C, else -> B -> C. C 는 하나만 두고 양쪽 가지에서 같이 쓴다.
        WorkflowNode branch = node("n1", FunctionType.CONDITION, 1);
        WorkflowNode aNode = node("n2", FunctionType.SQL, 2);
        WorkflowNode bNode = node("n3", FunctionType.SQL, 3);
        WorkflowNode mergedNode = node("n4", FunctionType.SQL, 4);

        WorkflowGraph graph = WorkflowGraph.of(
                List.of(branch, aNode, bNode, mergedNode),
                List.of(
                        edge("n1", "n2", BranchType.CASE, "c1", 0),
                        edge("n1", "n3", BranchType.ELSE, null, 1),
                        edge("n2", "n4", BranchType.DEFAULT, null, 2),
                        edge("n3", "n4", BranchType.DEFAULT, null, 3)
                ),
                List.of(condition("n1", "c1", "params[0].grade === 'A'", 0))
        );

        // 들어오는 연결이 둘이어도 시작 노드 판정에는 영향이 없다.
        assertEquals(branch, graph.getStartNode());

        assertEquals(mergedNode, graph.next(graph.nextCase(branch, "c1"), BranchType.DEFAULT));
        assertEquals(mergedNode, graph.next(graph.nextElse(branch), BranchType.DEFAULT));
        assertNull(graph.next(mergedNode, BranchType.DEFAULT));
    }

    @Test
    public void 삭제된_노드를_가리키는_연결은_흐름에서_빠진다() {
        WorkflowNode start = node("n1", FunctionType.SQL, 1);

        WorkflowGraph graph = WorkflowGraph.of(List.of(start), List.of(edge("n1", "삭제된노드", BranchType.DEFAULT, null, 0)));

        assertEquals(start, graph.getStartNode());
        assertNull(graph.next(start, BranchType.DEFAULT));
    }

    @Test
    public void 조건식_판정() throws ScriptEngineExecuteException {
        ConditionEvaluator evaluator = new ConditionEvaluator(new ScriptEngine());

        List<Map<String, Object>> params = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("status", "OK");
        row.put("count", 3);
        params.add(row);

        assertTrue(evaluator.evaluate("params[0].status === 'OK'", params));
        assertFalse(evaluator.evaluate("params[0].status === 'NG'", params));
        assertTrue(evaluator.evaluate("params.length > 0", params));
        assertTrue(evaluator.evaluate("if(params[0].count > 2) { return true; } return false;", params));
        assertFalse(evaluator.evaluate("params[0].count - 3", params));
        assertFalse(evaluator.evaluate("", params));

        // 없는 컬럼은 undefined 라 비교가 거짓이 된다.
        assertFalse(evaluator.evaluate("params[0].nothing === 'X'", params));

        // 빈 배열은 거짓, 비어있지 않은 문자열은 참이다. 문자열 'false' 도 참이므로 비교식으로 써야 한다.
        assertFalse(evaluator.evaluate("[]", params));
        assertTrue(evaluator.evaluate("'false'", params));
    }

    @Test
    public void 요청메시지_슬롯이_비어있으면_첫행_접근은_에러가_된다() throws ScriptEngineExecuteException {
        ConditionEvaluator evaluator = new ConditionEvaluator(new ScriptEngine());

        List<Map<String, Object>> empty = new ArrayList<>();

        // 앞 노드가 한 건도 못 돌려줬거나 요청메시지ID를 안 채우면 params 는 빈 배열이다.
        assertThrows(ScriptEngineExecuteException.class,
                () -> evaluator.evaluate("params[0].status === 'OK'", empty));

        // 그래서 길이 확인을 앞에 두면 에러 없이 거짓으로 흐른다.
        assertFalse(evaluator.evaluate("params.length > 0 && params[0].status === 'OK'", empty));
    }

    @Test
    public void 조건식이_잘못되면_예외로_알린다() {
        ConditionEvaluator evaluator = new ConditionEvaluator(new ScriptEngine());

        assertThrows(ScriptEngineExecuteException.class,
                () -> evaluator.evaluate("params[0].", new ArrayList<>()));
    }
}
