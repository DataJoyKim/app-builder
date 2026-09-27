package com.prometis.appbuilder.app.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    List<Workflow> findByApplicationId(String applicationId);

    Optional<Workflow> findByApplicationIdAndWorkflowCode(String applicationId, String workflowCode);
}
