package com.prometis.appbuilder.workflow;

import jakarta.persistence.*;
import lombok.*;

/**
 * 워크플로우에 허용한 IP 그룹. 권한(WorkflowAuthority)과 같은 방식으로 워크플로우에 매핑한다.
 * 한 건도 없으면 IP 제한을 쓰지 않는 워크플로우다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class WorkflowIpGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ipGroupCode;

    @ManyToOne
    @JoinColumn(name = "WORKFLOW_ID")
    private Workflow workflow;

    public void update(String ipGroupCode, Workflow workflow) {
        this.ipGroupCode = ipGroupCode;
        this.workflow = workflow;
    }
}
