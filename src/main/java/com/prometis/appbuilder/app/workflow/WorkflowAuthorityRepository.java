package com.prometis.appbuilder.app.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowAuthorityRepository extends JpaRepository<WorkflowAuthority, Long> {
    List<WorkflowAuthority> findByWorkflow(Workflow workflow);

    void deleteByWorkflowId(Long workflowId);
}
