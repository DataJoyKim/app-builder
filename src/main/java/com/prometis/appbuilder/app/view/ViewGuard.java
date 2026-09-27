package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.security.AppAuthenticationService;
import com.prometis.appbuilder.app.security.NotPermissionUserException;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.token.TokenCookie;
import com.prometis.appbuilder.app.security.appuser.AppUserAccessValidator;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.app.view.domain.Layout;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ViewGuard {
    private final AppAuthenticationService appAuthenticationService;
    private final ViewPermissionValidator viewPermissionValidator;
    private final AppUserAccessValidator appUserAccessValidator;

    /**
     * 인증만 체크
     * @param request
     * @param applicationId
     * @return
     * @throws SecurityBusinessException
     */
    public AuthenticatedUser check(HttpServletRequest request, String applicationId) throws SecurityBusinessException {
        return appAuthenticationService.authentication(applicationId, TokenCookie.resolveAccessToken(request));
    }

    /**
     * View Object 접근 가드
     * @param request
     * @param applicationId
     * @param viewObject
     * @return
     * @throws SecurityBusinessException
     * @throws BusinessException
     */
    public AuthenticatedUser check(HttpServletRequest request, String applicationId, ViewObject viewObject) throws BusinessException {
        AuthenticatedUser user = null;

        if(Boolean.TRUE.equals(viewObject.getUseAuthValidation())) {
            try {
                user = appAuthenticationService.authentication(applicationId, TokenCookie.resolveAccessToken(request));
            }
            catch (SecurityBusinessException e) {
                throw new BusinessException(ViewErrorMessage.FAILED_AUTHENTICATION);
            }

            try {
                appUserAccessValidator.validate(applicationId, user.getUserId());
            }
            catch (NotPermissionUserException e) {
                throw new BusinessException(ViewErrorMessage.NOT_FOUND_APP);
            }

            if(Boolean.TRUE.equals(viewObject.getUsePermissionValidation())) {
                viewPermissionValidator.validate(user, viewObject);
            }
        }

        return user;
    }

    /**
     * Layout 접근 가드
     * @param request
     * @param applicationId
     * @param layout
     * @return
     * @throws SecurityBusinessException
     * @throws BusinessException
     */
    public AuthenticatedUser check(HttpServletRequest request, String applicationId, Layout layout) throws BusinessException {
        AuthenticatedUser user = null;

        if(Boolean.TRUE.equals(layout.getUseAuthValidation())) {
            try {
                user = appAuthenticationService.authentication(applicationId, TokenCookie.resolveAccessToken(request));
            }
            catch (SecurityBusinessException e) {
                throw new BusinessException(ViewErrorMessage.FAILED_AUTHENTICATION);
            }

            try {
                appUserAccessValidator.validate(applicationId, user.getUserId());
            }
            catch (NotPermissionUserException e) {
                throw new BusinessException(ViewErrorMessage.NOT_FOUND_APP);
            }
        }

        return user;
    }
}
