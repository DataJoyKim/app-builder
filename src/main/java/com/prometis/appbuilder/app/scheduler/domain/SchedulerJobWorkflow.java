package com.prometis.appbuilder.app.scheduler.domain;

import com.prometis.appbuilder.app.workflow.Workflow;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class SchedulerJobWorkflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SCHEDULER_JOB_ID")
    private SchedulerJob schedulerJob;

    @ManyToOne
    @JoinColumn(name = "WORKFLOW_ID")
    private Workflow workflow;

    @Lob
    @Column
    private String requestMessageJson;

    @Column
    private Integer orderNum;

    public void update(Workflow workflow, String requestMessageJson, Integer orderNum) {
        this.workflow = workflow;
        this.requestMessageJson = requestMessageJson;
        this.orderNum = orderNum;
    }
}
