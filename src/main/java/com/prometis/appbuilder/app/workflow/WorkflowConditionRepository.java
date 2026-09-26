package com.prometis.appbuilder.app.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowConditionRepository extends JpaRepository<WorkflowCondition, Long> {
    List<WorkflowCondition> findByWorkflowId(Long workflowId);

    List<WorkflowCondition> findByWorkflowIdOrderByOrderNum(Long workflowId);

    void deleteByWorkflowId(Long workflowId);
}
