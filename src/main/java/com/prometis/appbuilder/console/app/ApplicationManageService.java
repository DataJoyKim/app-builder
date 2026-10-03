package com.prometis.appbuilder.console.app;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.app.security.appuser.AppUserService;
import com.prometis.appbuilder.console.app.dto.ApplicationCreateRequest;
import com.prometis.appbuilder.console.app.dto.OwnedApplicationsResponse;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationIdPolicy;
import com.prometis.appbuilder.platform.application.ApplicationOwnershipProperties;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.application.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 애플리케이션 콘솔에서 애플리케이션 관리자가 자신이 소유한(APPLICATION_ADMIN 인) 애플리케이션을 조회/생성한다.
 * 생성한 사용자는 그 애플리케이션의 관리자가 되며, 소유 개수는 platform.application.max-owned-count 를 넘을 수 없다.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ApplicationOwnershipProperties.class)
public class ApplicationManageService {
    private final ApplicationRepository applicationRepository;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final ApplicationIdPolicy applicationIdPolicy;
    private final ApplicationOwnershipProperties ownershipProperties;

    @Transactional(readOnly = true)
    public OwnedApplicationsResponse getOwnedApplications(Long userId) {
        List<Application> applications = findOwnedApplications(userId);

        return OwnedApplicationsResponse.builder()
                .applications(applications)
                .maxOwnedCount(ownershipProperties.maxOwnedCount())
                .creatable(applications.size() < ownershipProperties.maxOwnedCount())
                .build();
    }

    @Transactional
    public Application create(Long userId, ApplicationCreateRequest request) throws ApplicationManageException {
        if(findOwnedApplications(userId).size() >= ownershipProperties.maxOwnedCount()) {
            throw new ApplicationManageException("애플리케이션은 최대 " + ownershipProperties.maxOwnedCount() + "개까지 소유할 수 있습니다.");
        }

        String applicationId = request.getApplicationId() == null ? null : request.getApplicationId().trim();
        String idError = applicationIdPolicy.validate(applicationId);
        if(idError != null) {
            throw new ApplicationManageException(idError);
        }
        if(applicationRepository.findByApplicationId(applicationId).isPresent()) {
            throw new ApplicationManageException("이미 존재하는 애플리케이션ID입니다.");
        }
        if(request.getName() == null || request.getName().isBlank()) {
            throw new ApplicationManageException("애플리케이션명을 입력해주세요.");
        }

        Application saved = applicationRepository.save(Application.builder()
                .applicationId(applicationId)
                .name(request.getName().trim())
                .status(ApplicationStatus.ACTIVE.name())
                .description(request.getDescription())
                .build());

        appUserService.grantApplicationAdmin(applicationId, userId);

        return saved;
    }

    private List<Application> findOwnedApplications(Long userId) {
        List<String> applicationIds = appUserRepository.findByUserIdAndAuthority(userId, AppUser.AUTHORITY_APPLICATION_ADMIN).stream()
                .map(AppUser::getApplicationId)
                .toList();

        if(applicationIds.isEmpty()) {
            return List.of();
        }

        return applicationRepository.findByApplicationIdIn(applicationIds).stream()
                .sorted(Comparator.comparing(Application::getId))
                .toList();
    }
}
