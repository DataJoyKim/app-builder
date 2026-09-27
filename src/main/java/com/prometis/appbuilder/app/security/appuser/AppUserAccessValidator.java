package com.prometis.appbuilder.app.security.appuser;

import com.prometis.appbuilder.app.security.NotPermissionUserException;
import com.prometis.appbuilder.app.workflow.WorkflowErrorMessage;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import com.prometis.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserAccessValidator {
    private final AppUserRepository appUserRepository;
    private final UserRepository userRepository;

    public void validate(String applicationId, Long userId) throws NotPermissionUserException {
        // 플랫폼관리자 경우 검증 pass
        Optional<User> userOptional = userRepository.findByIdAndAuthority(userId, User.AUTHORITY_PLATFORM_ADMIN);
        if(userOptional.isPresent()) {
            return;
        }

        // 앱 사용자가 아니면 접근불가
        Optional<AppUser> appUserOptional = appUserRepository.findByApplicationIdAndUserId(applicationId, userId);
        if(appUserOptional.isEmpty()) {
            throw new NotPermissionUserException();
        }
    }
}
