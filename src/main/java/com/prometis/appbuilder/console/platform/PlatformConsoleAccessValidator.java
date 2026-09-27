package com.prometis.appbuilder.console.platform;

import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PlatformConsoleAccessValidator {

    private final UserRepository userRepository;

    public void validate(Long userId) throws NotPlatformAdminException {
        Optional<User> userOptional = userRepository.findByIdAndAuthority(userId, User.AUTHORITY_PLATFORM_ADMIN);
        if(userOptional.isEmpty()) {
            throw new NotPlatformAdminException();
        }
    }
}
