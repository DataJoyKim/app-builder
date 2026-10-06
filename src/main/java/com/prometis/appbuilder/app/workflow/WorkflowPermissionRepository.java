package com.prometis.appbuilder.app.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowPermissionRepository extends JpaRepository<WorkflowPermission, Long> {
    List<WorkflowPermission> findByWorkflow(Workflow workflow);

    void deleteByWorkflowId(Long workflowId);

    // 권한 화면: 이 권한으로 실행할 수 있는 이 애플리케이션의 워크플로우
    List<WorkflowPermission> findByPermissionCodeAndWorkflow_ApplicationId(String permissionCode, String applicationId);

    boolean existsByWorkflowIdAndPermissionCode(Long workflowId, String permissionCode);
}
