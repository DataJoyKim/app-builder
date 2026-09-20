package com.prometis.appbuilder.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowIpGroupRepository extends JpaRepository<WorkflowIpGroup, Long> {
    List<WorkflowIpGroup> findByWorkflow(Workflow workflow);

    List<WorkflowIpGroup> findByIpGroupCode(String ipGroupCode);

    void deleteByWorkflowId(Long workflowId);
}
