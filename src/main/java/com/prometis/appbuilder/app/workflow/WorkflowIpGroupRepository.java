package com.prometis.appbuilder.app.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowIpGroupRepository extends JpaRepository<WorkflowIpGroup, Long> {
    List<WorkflowIpGroup> findByWorkflow(Workflow workflow);

    // IP 그룹 코드는 애플리케이션 안에서만 유일하므로 워크플로우의 애플리케이션으로 좁힌다.
    List<WorkflowIpGroup> findByWorkflow_ApplicationIdAndIpGroupCode(String applicationId, String ipGroupCode);

    void deleteByWorkflowId(Long workflowId);
}
