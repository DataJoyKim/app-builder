package com.prometis.appbuilder.console.app;

import com.prometis.appbuilder.app.security.NotAppAdminException;
import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AppConsoleAccessValidator {

    private final UserRepository userRepository;
    private final AppUserRepository appUserRepository;

    public void validate(String applicationId, Long userId) throws NotAppAdminException {
        // 플랫폼관리자 경우 검증 pass
        Optional<User> userOptional = userRepository.findByIdAndAuthority(userId, User.AUTHORITY_PLATFORM_ADMIN);
        if(userOptional.isPresent()) {
            return;
        }

        // 앱 관리자가 아니면 접근불가
        Optional<AppUser> appUserOptional = appUserRepository.findByUserIdAndApplicationIdAndAuthority(userId, applicationId, AppUser.AUTHORITY_APPLICATION_ADMIN);
        if(appUserOptional.isEmpty()) {
            throw new NotAppAdminException();
        }
    }
}
