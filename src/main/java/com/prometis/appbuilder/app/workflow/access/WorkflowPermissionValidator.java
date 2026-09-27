package com.prometis.appbuilder.app.workflow.access;

import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.domain.GrantedPermission;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowErrorMessage;
import com.prometis.appbuilder.app.workflow.WorkflowPermission;
import com.prometis.appbuilder.app.workflow.WorkflowPermissionRepository;
import com.prometis.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowPermissionValidator {
    private final WorkflowPermissionRepository workflowPermissionRepository;

    public void validate(AuthenticatedUser user, Workflow workflow) throws BusinessException {
        List<WorkflowPermission> workflowPermissions = workflowPermissionRepository.findByWorkflow(workflow);
        if(workflowPermissions.isEmpty()) {
            throw new BusinessException(WorkflowErrorMessage.NOT_SETTING_PERMISSION);
        }

        Map<String, Workflow> permissionMap = new HashMap<>();
        for(WorkflowPermission workflowPermission : workflowPermissions) {
            permissionMap.put(workflowPermission.getPermissionCode(), workflowPermission.getWorkflow());
        }

        if(permissionMap.containsKey(WorkflowPermission.VALID_PASS)) {
            return;
        }

        List<GrantedPermission> grantedPermissions = user.getGrantedPermissions();
        if(grantedPermissions.isEmpty()) {
            throw new BusinessException(WorkflowErrorMessage.NOT_HAS_PERMISSIONS);
        }

        boolean hasPermission = false;
        for(GrantedPermission permission : grantedPermissions){
            if(permissionMap.containsKey(permission.getRole())) {
                hasPermission = true;
                break;
            }
        }

        if(!hasPermission) {
            throw new BusinessException(WorkflowErrorMessage.PERMISSION_DENIED);
        }
    }
}
