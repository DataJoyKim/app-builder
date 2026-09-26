package com.prometis.appbuilder.app.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class SchedulerJobHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SCHEDULER_JOB_ID")
    private SchedulerJob schedulerJob;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;

    @Column
    private Boolean success;

    @Lob
    @Column
    private String message;

    public void finish(Boolean success, String message) {
        this.finishedAt = LocalDateTime.now();
        this.success = success;
        this.message = message;
    }
}
