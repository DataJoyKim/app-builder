package com.prometis.appbuilder.app.security;

import com.prometis.appbuilder.app.security.session.AppSession;
import com.prometis.appbuilder.app.security.session.AppSessionService;
import com.prometis.appbuilder.app.security.usergroup.*;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppAuthenticationService {
    private final UserGroupService userGroupService;
    private final UserGroupPermissionRepository userGroupPermissionRepository;
    private final AuthenticationService authenticationService;
    private final AppSessionService appSessionService;

    /**
     * 토큰으로 사용자를 확인하고, applicationId 애플리케이션의 사용자 그룹에서만 권한을 모아 부여한다.
     * 사용자 그룹/권한은 애플리케이션마다 따로라서, 다른 애플리케이션에서 받은 권한이 섞이면 안 된다.
     */
    @Transactional(readOnly = true)
    public AuthenticatedUser authentication(String applicationId, String accessToken) throws SecurityBusinessException {
        AuthenticatedUser authenticatedUser = authenticationService.authentication(accessToken);

        AppSession session = appSessionService.registerSession(applicationId, authenticatedUser);

        // 사용자 권한 부여
        List<UserGroupUser> userGroupUsers = userGroupService.getUserGroupUser(authenticatedUser.getUserId());

        for(UserGroupUser userGroupUser : userGroupUsers) {
            UserGroup userGroup = userGroupUser.getUserGroup();
            if(
                userGroup == null ||
                !applicationId.equals(userGroup.getApplicationId()) ||
                !session.getCompanyCode().equals(userGroup.getCompanyCode())
            ) {
                continue;
            }

            grantUserGroupPermissions(authenticatedUser, userGroup);
        }

        return authenticatedUser;
    }

    // 자신이 속한 그룹의 권한은 그대로, 상위 그룹의 권한은 하위 전파(lowerPermissionGrant)로 설정된 것만 부여한다.
    private void grantUserGroupPermissions(AuthenticatedUser authenticatedUser, UserGroup userGroup) {
        List<UserGroupPermission> userGroupPermissions = userGroupPermissionRepository.findByUserGroupId(userGroup.getId());
        for(UserGroupPermission userGroupPermission : userGroupPermissions) {
            if(userGroupPermission.getPermission() == null) {
                continue;
            }

            authenticatedUser.grantPermission(userGroupPermission.getPermission().getCode());
        }

        for(UserGroup ancestor : userGroup.getAncestors()) {
            List<UserGroupPermission> ancestorPermissions = userGroupPermissionRepository.findByUserGroupId(ancestor.getId());

            for(UserGroupPermission ancestorPermission : ancestorPermissions) {
                if(Boolean.TRUE.equals(ancestorPermission.getLowerPermissionGrant()) && ancestorPermission.getPermission() != null) {
                    authenticatedUser.grantPermission(ancestorPermission.getPermission().getCode());
                }
            }
        }
    }
}
