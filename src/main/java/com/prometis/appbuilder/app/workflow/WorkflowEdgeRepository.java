package com.prometis.appbuilder.app.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowEdgeRepository extends JpaRepository<WorkflowEdge, Long> {
    List<WorkflowEdge> findByWorkflowId(Long workflowId);

    List<WorkflowEdge> findByWorkflowIdOrderByOrderNum(Long workflowId);

    void deleteByWorkflowId(Long workflowId);
}
