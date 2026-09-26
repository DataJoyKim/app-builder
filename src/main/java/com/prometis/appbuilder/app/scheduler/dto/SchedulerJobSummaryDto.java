package com.prometis.appbuilder.app.scheduler.dto;

import com.prometis.appbuilder.app.scheduler.domain.SchedulerJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor @Builder
public class SchedulerJobSummaryDto {
    private Long id;
    private String jobCode;
    private String jobName;
    private String cronExpression;
    private String description;
    private Boolean enabled;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime nextFireTime;

    public static SchedulerJobSummaryDto of(SchedulerJob job, LocalDateTime lastExecutedAt, LocalDateTime nextFireTime) {
        return SchedulerJobSummaryDto.builder()
                .id(job.getId())
                .jobCode(job.getJobCode())
                .jobName(job.getJobName())
                .cronExpression(job.getCronExpression())
                .description(job.getDescription())
                .enabled(job.getEnabled())
                .lastExecutedAt(lastExecutedAt)
                .nextFireTime(nextFireTime)
                .build();
    }
}
