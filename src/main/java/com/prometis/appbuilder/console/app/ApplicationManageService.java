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
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 애플리케이션 콘솔에서 애플리케이션 관리자가 자신이 소유한(APPLICATION_ADMIN 인) 애플리케이션을 조회/생성한다.
 * 생성한 사용자는 그 애플리케이션의 관리자가 되며, 소유 개수는 platform.application.max-owned-count 를 넘을 수 없다.
 * 애플리케이션을 만들 수 있는 사람은 users.authority 가 APPLICATION_ADMIN 인 사용자뿐이다 (공개 가입으로 가입하면 받는다).
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ApplicationOwnershipProperties.class)
public class ApplicationManageService {
    public static final String NOT_APPLICATION_ADMIN_MESSAGE = "애플리케이션 관리자 권한이 있는 사용자만 애플리케이션을 만들 수 있습니다.";

    private final ApplicationRepository applicationRepository;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final ApplicationIdPolicy applicationIdPolicy;
    private final ApplicationOwnershipProperties ownershipProperties;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public OwnedApplicationsResponse getOwnedApplications(Long userId) {
        List<Application> applications = findOwnedApplications(userId);

        String notCreatableReason = null;
        if(!canCreateApplication(userId)) {
            notCreatableReason = NOT_APPLICATION_ADMIN_MESSAGE;
        }
        else if(applications.size() >= ownershipProperties.maxOwnedCount()) {
            notCreatableReason = maxOwnedMessage();
        }

        return OwnedApplicationsResponse.builder()
                .applications(applications)
                .maxOwnedCount(ownershipProperties.maxOwnedCount())
                .creatable(notCreatableReason == null)
                .notCreatableReason(notCreatableReason)
                .build();
    }

    @Transactional
    public Application create(Long userId, ApplicationCreateRequest request) throws ApplicationManageException {
        if(!canCreateApplication(userId)) {
            throw new ApplicationManageException(NOT_APPLICATION_ADMIN_MESSAGE);
        }
        if(findOwnedApplications(userId).size() >= ownershipProperties.maxOwnedCount()) {
            throw new ApplicationManageException(maxOwnedMessage());
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

    private String maxOwnedMessage() {
        return "애플리케이션은 최대 " + ownershipProperties.maxOwnedCount() + "개까지 소유할 수 있습니다.";
    }

    /** users.authority 가 APPLICATION_ADMIN 인 사용자만 애플리케이션을 만들고 /applications/manage 에 들어갈 수 있다 */
    @Transactional(readOnly = true)
    public boolean canCreateApplication(Long userId) {
        return userId != null && userRepository.findByIdAndAuthority(userId, User.AUTHORITY_APPLICATION_ADMIN).isPresent();
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
