package com.prometis.appbuilder.app.workflow;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="WORKFLOW_UQ",columnNames={"WORKFLOW_CODE"})})
@Entity
public class Workflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String workflowCode;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Boolean useAuthValidation;

    public void update(String workflowCode, String displayName, String note, Boolean useAuthValidation) {
        this.workflowCode = workflowCode;
        this.displayName = displayName;
        this.note = note;
        this.useAuthValidation = useAuthValidation;
    }
}
