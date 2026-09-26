package com.prometis.appbuilder.app.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowErrorResponseRepository extends JpaRepository<WorkflowErrorResponse, Long> {
    List<WorkflowErrorResponse> findByWorkflowId(Long workflowId);

    void deleteByWorkflowId(Long workflowId);
}
