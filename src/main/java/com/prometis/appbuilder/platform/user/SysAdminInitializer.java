package com.prometis.appbuilder.platform.user;

import com.prometis.core.crypto.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서버 기동 시 플랫폼 관리자(sysadmin) 계정이 없으면 만든다.
 * 이미 있으면 비밀번호를 포함해 아무것도 바꾸지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(SysAdminProperties.class)
public class SysAdminInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SysAdminProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String loginId = properties.loginId();

        if(userRepository.findByLoginId(loginId).isPresent()) {
            return;
        }

        String initialPassword = properties.initialPassword();
        if(initialPassword == null || initialPassword.isBlank()) {
            throw new IllegalStateException("플랫폼 관리자(" + loginId + ") 계정을 만들 초기 비밀번호가 없습니다. application.yml 의 platform.sysadmin.initial-password 를 설정해주세요.");
        }

        userRepository.save(User.builder()
                .loginId(loginId)
                .userName(properties.userName())
                .email(properties.email())
                .authority(UserAuthority.PLATFORM_ADMIN.name())
                .password(passwordEncoder.encode(initialPassword))
                .build());

        log.info("플랫폼 관리자({}) 계정을 생성했습니다.", loginId);
    }
}
