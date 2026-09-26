package com.prometis.appbuilder.app.workflow;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class WorkflowAuthority {
    public static final String VALID_PASS = "VALID_PASS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String authorityCode;

    @ManyToOne
    @JoinColumn(name = "WORKFLOW_ID")
    private Workflow workflow;

    public void update(String authorityCode, Workflow workflow) {
        this.authorityCode = authorityCode;
        this.workflow = workflow;
    }
}
