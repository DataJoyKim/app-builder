package com.prometis.appbuilder.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowDomainRepository extends JpaRepository<WorkflowDomain, Long> {
    List<WorkflowDomain> findByWorkflow(Workflow workflow);

    void deleteByWorkflowId(Long workflowId);
}
