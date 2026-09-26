package com.prometis.appbuilder.app.workflow;

import jakarta.persistence.*;
import lombok.*;

/**
 * 워크플로우에 허용한 도메인. 권한(WorkflowAuthority)과 같은 방식으로 워크플로우에 매핑한다.
 * 한 건도 없으면 도메인 제한을 쓰지 않는 워크플로우다.
 * domain 에는 정확한 도메인(portal.example.com) 외에 와일드카드(*.example.com, dev-*.example.com, *)도 쓸 수 있다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class WorkflowDomain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String domain;

    @ManyToOne
    @JoinColumn(name = "WORKFLOW_ID")
    private Workflow workflow;

    public void update(String domain, Workflow workflow) {
        this.domain = domain;
        this.workflow = workflow;
    }
}
