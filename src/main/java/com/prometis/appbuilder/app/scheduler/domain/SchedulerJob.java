package com.prometis.appbuilder.app.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="SCHEDULER_JOB_UQ",columnNames={"jobCode"})})
@Entity
public class SchedulerJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String jobCode;

    @Column(nullable = false, length = 200)
    private String jobName;

    @Column(nullable = false, length = 100)
    private String cronExpression;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean enabled;

    public void update(String jobCode, String jobName, String cronExpression, String description, Boolean enabled) {
        this.jobCode = jobCode;
        this.jobName = jobName;
        this.cronExpression = cronExpression;
        this.description = description;
        this.enabled = enabled;
    }
}
