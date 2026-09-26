package com.prometis.appbuilder.app.node;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, Long> {
    List<WorkflowNode> findByWorkflowIdOrderByOrderNum(Long workflowId);

    void deleteByWorkflowId(Long workflowId);

    List<WorkflowNode> findByWorkflowId(Long workflowId);
}
