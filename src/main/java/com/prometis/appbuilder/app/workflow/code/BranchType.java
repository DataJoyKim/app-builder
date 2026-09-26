package com.prometis.appbuilder.app.workflow.code;

/**
 * 노드와 노드를 잇는 연결선(WorkflowEdge)의 분기 종류.
 * - DEFAULT : 일반 노드의 다음 흐름. 노드당 하나다.
 * - CASE    : 조건분기 노드의 조건(WorkflowCondition) 하나에 딸린 가지. branchId 로 어느 조건인지 가린다.
 * - ELSE    : 조건분기 노드에서 어떤 조건에도 걸리지 않았을 때의 가지.
 */
public enum BranchType {
    DEFAULT, CASE, ELSE
}
