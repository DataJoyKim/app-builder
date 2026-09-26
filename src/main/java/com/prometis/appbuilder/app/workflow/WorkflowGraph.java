package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.node.WorkflowNode;
import com.prometis.appbuilder.app.workflow.code.BranchType;

import java.util.*;

/**
 * 워크플로우의 노드와 연결정보를 실행 가능한 그래프로 조립한다.
 * 흐름은 오직 연결정보(WorkflowEdge)로만 정해진다. orderNum 은 빌더의 표시 순서일 뿐 실행 순서가 아니다.
 */
public class WorkflowGraph {
    public static final int MAX_EXECUTE_STEP = 1000; // 최대 실행 노드 수 설정

    private final Map<String, WorkflowNode> nodes;
    private final Map<String, List<WorkflowEdge>> outgoingEdges;
    private final Map<String, List<WorkflowCondition>> conditions;
    private final Map<String, WorkflowErrorResponse> errorResponses;
    private final WorkflowNode startNode;

    private WorkflowGraph(
            Map<String, WorkflowNode> nodes,
            Map<String, List<WorkflowEdge>> outgoingEdges,
            Map<String, List<WorkflowCondition>> conditions,
            Map<String, WorkflowErrorResponse> errorResponses,
            WorkflowNode startNode
    ) {
        this.nodes = nodes;
        this.outgoingEdges = outgoingEdges;
        this.conditions = conditions;
        this.errorResponses = errorResponses;
        this.startNode = startNode;
    }

    public static WorkflowGraph of(List<WorkflowNode> functions, List<WorkflowEdge> edges) {
        return of(functions, edges, List.of());
    }

    public static WorkflowGraph of(
            List<WorkflowNode> functions,
            List<WorkflowEdge> edges,
            List<WorkflowCondition> conditions
    ) {
        return of(functions, edges, conditions, List.of());
    }

    public static WorkflowGraph of(
            List<WorkflowNode> functions,
            List<WorkflowEdge> edges,
            List<WorkflowCondition> conditions,
            List<WorkflowErrorResponse> errorResponses
    ) {
        List<WorkflowNode> orderedFunctions = sortByOrderNum(functions);

        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        for(WorkflowNode function : orderedFunctions) {
            nodes.put(function.getNodeId(), function);
        }

        Map<String, List<WorkflowEdge>> outgoingEdges = new HashMap<>();
        Set<String> targetNodeIds = new HashSet<>();

        for(WorkflowEdge edge : (edges == null ? List.<WorkflowEdge>of() : edges)) {
            // 삭제된 노드를 가리키는 끊어진 연결은 흐름에서 제외한다.
            if(!nodes.containsKey(edge.getSourceNodeId()) || !nodes.containsKey(edge.getTargetNodeId())) {
                continue;
            }

            outgoingEdges.computeIfAbsent(edge.getSourceNodeId(), key -> new ArrayList<>()).add(edge);
            targetNodeIds.add(edge.getTargetNodeId());
        }

        for(List<WorkflowEdge> nodeEdges : outgoingEdges.values()) {
            nodeEdges.sort(Comparator.comparing(WorkflowEdge::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        return new WorkflowGraph(
                nodes,
                outgoingEdges,
                resolveConditions(nodes, conditions),
                resolveErrorResponses(nodes, errorResponses),
                findStartNode(nodes, targetNodeIds)
        );
    }

    public WorkflowNode getStartNode() {
        return startNode;
    }

    /**
     * 조건분기 노드가 가진 조건들을 판정 순서대로 돌려준다.
     */
    public List<WorkflowCondition> conditionsOf(WorkflowNode node) {
        return conditions.getOrDefault(node.getNodeId(), List.of());
    }

    /**
     * 에러메시지 노드의 설정. 설정 없이 저장된 노드면 null.
     */
    public WorkflowErrorResponse errorResponseOf(WorkflowNode node) {
        return errorResponses.get(node.getNodeId());
    }

    /**
     * 참이 된 조건의 가지.
     */
    public WorkflowNode nextCase(WorkflowNode node, String branchId) {
        for(WorkflowEdge edge : edgesOf(node)) {
            if(BranchType.CASE.equals(edge.getBranchType()) && Objects.equals(branchId, edge.getBranchId())) {
                return nodes.get(edge.getTargetNodeId());
            }
        }

        return null;
    }

    /**
     * 어떤 조건에도 걸리지 않았을 때의 가지.
     */
    public WorkflowNode nextElse(WorkflowNode node) {
        for(WorkflowEdge edge : edgesOf(node)) {
            if(BranchType.ELSE.equals(edge.getBranchType())) {
                return nodes.get(edge.getTargetNodeId());
            }
        }

        return null;
    }

    public WorkflowNode next(WorkflowNode current, BranchType branchType) {
        List<WorkflowEdge> candidates = edgesOf(current);
        if(candidates.isEmpty()) {
            return null;
        }

        for(WorkflowEdge edge : candidates) {
            if(branchType.equals(edge.getBranchType())) {
                return nodes.get(edge.getTargetNodeId());
            }
        }

        // 분기값 없이 저장된 연결선이라도 나가는 길이 하나뿐이면 직선 흐름으로 본다.
        if(BranchType.DEFAULT.equals(branchType) && candidates.size() == 1 && !isConditionBranch(candidates.get(0))) {
            return nodes.get(candidates.get(0).getTargetNodeId());
        }

        return null;
    }

    public int nodeSize() {
        return nodes.size();
    }

    private List<WorkflowEdge> edgesOf(WorkflowNode node) {
        return outgoingEdges.getOrDefault(node.getNodeId(), List.of());
    }

    private static boolean isConditionBranch(WorkflowEdge edge) {
        return BranchType.CASE.equals(edge.getBranchType()) || BranchType.ELSE.equals(edge.getBranchType());
    }

    private static Map<String, List<WorkflowCondition>> resolveConditions(
            Map<String, WorkflowNode> nodes,
            List<WorkflowCondition> conditions
    ) {
        Map<String, List<WorkflowCondition>> conditionsByNode = new HashMap<>();

        for(WorkflowCondition condition : (conditions == null ? List.<WorkflowCondition>of() : conditions)) {
            if(!nodes.containsKey(condition.getNodeId())) {
                continue;
            }

            conditionsByNode.computeIfAbsent(condition.getNodeId(), key -> new ArrayList<>()).add(condition);
        }

        for(List<WorkflowCondition> nodeConditions : conditionsByNode.values()) {
            nodeConditions.sort(Comparator.comparing(WorkflowCondition::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        return conditionsByNode;
    }

    private static Map<String, WorkflowErrorResponse> resolveErrorResponses(
            Map<String, WorkflowNode> nodes,
            List<WorkflowErrorResponse> errorResponses
    ) {
        Map<String, WorkflowErrorResponse> errorResponsesByNode = new HashMap<>();

        for(WorkflowErrorResponse errorResponse : (errorResponses == null ? List.<WorkflowErrorResponse>of() : errorResponses)) {
            if(!nodes.containsKey(errorResponse.getNodeId())) {
                continue;
            }

            errorResponsesByNode.put(errorResponse.getNodeId(), errorResponse);
        }

        return errorResponsesByNode;
    }

    private static WorkflowNode findStartNode(Map<String, WorkflowNode> nodes, Set<String> targetNodeIds) {
        // 시작 노드는 들어오는 연결이 없는 노드다. 순환 등으로 후보가 없으면 순서상 첫 노드로 시작한다.
        for(Map.Entry<String, WorkflowNode> node : nodes.entrySet()) {
            if(!targetNodeIds.contains(node.getKey())) {
                return node.getValue();
            }
        }

        return nodes.isEmpty() ? null : nodes.values().iterator().next();
    }

    private static List<WorkflowNode> sortByOrderNum(List<WorkflowNode> functions) {
        List<WorkflowNode> orderedFunctions = new ArrayList<>(functions == null ? List.of() : functions);

        orderedFunctions.sort(Comparator
                .comparing(WorkflowNode::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkflowNode::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        return orderedFunctions;
    }
}
