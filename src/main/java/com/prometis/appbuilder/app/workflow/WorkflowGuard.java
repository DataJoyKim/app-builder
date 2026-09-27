package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.security.AppAuthenticationService;
import com.prometis.appbuilder.app.security.NotPermissionUserException;
import com.prometis.appbuilder.app.security.appuser.AppUserAccessValidator;
import com.prometis.appbuilder.app.workflow.access.HostAccessValidator;
import com.prometis.appbuilder.app.workflow.access.IpAccessValidator;
import com.prometis.appbuilder.app.workflow.access.WorkflowPermissionValidator;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.token.TokenCookie;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowGuard {
    private final AppAuthenticationService appAuthenticationService;
    private final IpAccessValidator ipAccessValidator;
    private final HostAccessValidator hostAccessValidator;
    private final WorkflowPermissionValidator workflowPermissionValidator;
    private final AppUserAccessValidator appUserAccessValidator;

    public AuthenticatedUser check(HttpServletRequest request, String applicationId, Workflow workflow) throws BusinessException {
        AuthenticatedUser user = null;

        if(workflow.getUseAuthValidation()) {
            try {
                user = appAuthenticationService.authentication(applicationId, TokenCookie.resolveAccessToken(request));
            }
            catch (SecurityBusinessException e) {
                throw new BusinessException(WorkflowErrorMessage.FAILED_AUTHENTICATION);
            }
        }

        // 애플리케이션 유저 검증
        if(user != null) {
            try {
                appUserAccessValidator.validate(applicationId, user.getUserId());
            }
            catch (NotPermissionUserException e) {
                throw new BusinessException(WorkflowErrorMessage.PERMISSION_DENIED);
            }
        }

        // 리소스 접근제어
        if(user != null) {
            workflowPermissionValidator.validate(user, workflow);
        }

        // IP 접근제어
        ipAccessValidator.validate(request, workflow);

        // 도메인 접근제어.
        hostAccessValidator.validate(request, workflow);

        return user;
    }
}
