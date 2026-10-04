package com.prometis.appbuilder.platform.home;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.application.ApplicationStatus;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 루트(/) 진입 시 사용자 유형을 가린다.
 * - 플랫폼관리자 : users.authority 가 PLATFORM_ADMIN
 * - 애플리케이션 관리자 : 하나 이상의 애플리케이션에서 APPLICATION_ADMIN 인 사용자
 * - 일반 사용자 : 애플리케이션 사용자(APPLICATION_USER)로만 등록된 사용자
 * - 그 밖에 어느 애플리케이션에도 없는 사용자(공개 가입 직후 등)는 자기 애플리케이션을 만드는 화면으로 보낸다
 * 공개 가입(/signup)으로 누구나 계정을 만들 수 있으므로, 애플리케이션 생성(/applications/manage)은 로그인한 사용자면 누구나 할 수 있다.
 * (소유 개수는 platform.application.max-owned-count 로 제한된다)
 */
@Service
@RequiredArgsConstructor
public class HomeService {
    private final UserRepository userRepository;
    private final AppUserRepository appUserRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public HomeLanding landingOf(Long userId) {
        if(isPlatformAdmin(userId)) {
            return HomeLanding.PLATFORM_CONSOLE;
        }
        if(isApplicationAdmin(userId)) {
            return HomeLanding.APPLICATION_MANAGE;
        }
        if(appUserRepository.findByUserId(userId).isEmpty()) {
            return HomeLanding.APPLICATION_MANAGE;
        }
        return HomeLanding.APPLICATION_SELECT;
    }

    /**
     * 사용자가 가입되어 있는(AppUser 로 등록된) 사용 중인 애플리케이션 목록. 미사용(INACTIVE) 애플리케이션은 고를 수 없으므로 뺀다.
     */
    @Transactional(readOnly = true)
    public List<Application> getJoinedApplications(Long userId) {
        List<String> applicationIds = appUserRepository.findByUserId(userId).stream()
                .map(AppUser::getApplicationId)
                .distinct()
                .toList();

        if(applicationIds.isEmpty()) {
            return List.of();
        }

        return applicationRepository.findByApplicationIdIn(applicationIds).stream()
                .filter(application -> !ApplicationStatus.INACTIVE.name().equals(application.getStatus()))
                .sorted(Comparator.comparing(Application::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean isPlatformAdmin(Long userId) {
        return userRepository.findByIdAndAuthority(userId, User.AUTHORITY_PLATFORM_ADMIN).isPresent();
    }

    private boolean isApplicationAdmin(Long userId) {
        return !appUserRepository.findByUserIdAndAuthority(userId, AppUser.AUTHORITY_APPLICATION_ADMIN).isEmpty();
    }
}
