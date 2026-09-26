package com.prometis.appbuilder.app.workflow;

import jakarta.persistence.*;
import lombok.*;

/**
 * 조건분기(CONDITION) 노드가 가진 조건 하나. if / else if 가 각각 한 행이다.
 * orderNum 순서대로 판정해서 처음 참이 된 조건의 가지(branchId 가 같은 CASE 엣지)로 흐르고,
 * 전부 거짓이면 ELSE 엣지로 흐른다.
 * branchId 는 한 노드 안에서 조건과 연결선을 묶어주는 값이라, 조건 순서를 바꿔도 가지가 따라 움직인다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class WorkflowCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "WORKFLOW_ID", nullable = false)
    private Long workflowId;

    @Column(nullable = false, length = 100)
    private String nodeId;

    @Column(nullable = false, length = 100)
    private String branchId;

    @Column(length = 2000)
    private String conditionExpression;

    @Column
    private Integer orderNum;

    public void update(
            Long workflowId,
            String nodeId,
            String branchId,
            String conditionExpression,
            Integer orderNum
    ) {
        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.branchId = branchId;
        this.conditionExpression = conditionExpression;
        this.orderNum = orderNum;
    }
}
