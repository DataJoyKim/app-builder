package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.workflow.code.BranchType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 워크플로우 노드(WorkflowNode) 간의 연결 정보.
 * 예전에는 WorkflowNode.orderNum 만으로 실행 순서를 정했기 때문에 직선 흐름밖에 표현할 수 없었다.
 * 조건분기를 표현하기 위해 노드는 nodeId 로 식별하고, 흐름은 이 엣지(sourceNodeId -> targetNodeId)로 구성한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class WorkflowEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "WORKFLOW_ID", nullable = false)
    private Long workflowId;

    @Column(nullable = false, length = 100)
    private String sourceNodeId;

    @Column(nullable = false, length = 100)
    private String targetNodeId;

    // varchar 로 못박아둔다. 그냥 두면 하이버네이트가 H2 의 ENUM 타입으로 컬럼을 만드는데,
    // ddl-auto: update 는 이미 만들어진 ENUM 의 허용값 목록을 넓혀주지 않는다.
    // 그래서 BranchType 에 값을 하나 추가하는 순간 기존 DB 에서는 "Value not permitted for column" 으로 저장이 깨진다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private BranchType branchType;

    // CASE 엣지일 때 어느 조건(WorkflowCondition)의 가지인지 가리킨다. 그 외에는 비어있다.
    @Column(length = 100)
    private String branchId;

    @Column
    private Integer orderNum;

    public void update(
            Long workflowId,
            String sourceNodeId,
            String targetNodeId,
            BranchType branchType,
            String branchId,
            Integer orderNum
    ) {
        this.workflowId = workflowId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.branchType = branchType;
        this.branchId = branchId;
        this.orderNum = orderNum;
    }
}
