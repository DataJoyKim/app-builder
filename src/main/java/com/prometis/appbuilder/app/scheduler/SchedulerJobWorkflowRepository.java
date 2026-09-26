package com.prometis.appbuilder.app.scheduler;

import com.prometis.appbuilder.app.scheduler.domain.SchedulerJobWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchedulerJobWorkflowRepository extends JpaRepository<SchedulerJobWorkflow, Long> {
    List<SchedulerJobWorkflow> findBySchedulerJobIdOrderByOrderNum(Long schedulerJobId);

    void deleteBySchedulerJobId(Long schedulerJobId);
}
