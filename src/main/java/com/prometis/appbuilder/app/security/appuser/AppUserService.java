package com.prometis.appbuilder.app.security.appuser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppUserService {
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<AppUser> getAppUsers(String applicationId) {
        return appUserRepository.findByApplicationId(applicationId);
    }

    /**
     * 사용자들을 애플리케이션 사용자(APPLICATION_USER)로 등록한다.
     * 이미 등록된 사용자는 건너뛴다. (관리자의 권한을 사용자로 낮추지 않기 위함)
     * @return 새로 등록한 행
     */
    @Transactional
    public List<AppUser> registerAppUsers(String applicationId, List<Long> userIds) {
        Set<Long> registered = appUserRepository.findByApplicationId(applicationId).stream()
                .map(AppUser::getUserId)
                .collect(Collectors.toSet());

        List<AppUser> newAppUsers = new LinkedHashSet<>(userIds).stream()
                .filter(userId -> userId != null && !registered.contains(userId))
                .map(userId -> AppUser.builder()
                        .applicationId(applicationId)
                        .userId(userId)
                        .authority(AppUser.AUTHORITY_APPLICATION_USER)
                        .build())
                .toList();

        return appUserRepository.saveAll(newAppUsers);
    }

    @Transactional(readOnly = true)
    public List<AppUser> getApplicationAdmins(String applicationId) {
        return appUserRepository.findByApplicationIdAndAuthority(applicationId, AppUser.AUTHORITY_APPLICATION_ADMIN);
    }

    /**
     * 사용자를 애플리케이션 관리자로 지정한다.
     * 애플리케이션 사용자는 (applicationId, userId) 당 한 행이라, 이미 있으면 권한만 APPLICATION_ADMIN 으로 바꾼다.
     */
    @Transactional
    public AppUser grantApplicationAdmin(String applicationId, Long userId) {
        Optional<AppUser> appUserOptional = appUserRepository.findByApplicationIdAndUserId(applicationId, userId);
        if(appUserOptional.isPresent()) {
            AppUser appUser = appUserOptional.get();
            appUser.changeAuthority(AppUser.AUTHORITY_APPLICATION_ADMIN);
            return appUser;
        }

        return appUserRepository.save(AppUser.builder()
                .applicationId(applicationId)
                .userId(userId)
                .authority(AppUser.AUTHORITY_APPLICATION_ADMIN)
                .build());
    }
}
