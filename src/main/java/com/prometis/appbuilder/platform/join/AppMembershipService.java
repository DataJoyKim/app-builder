package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.app.security.appuser.AppUserService;
import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUser;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUserRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 애플리케이션 가입(멤버십). 초대 합류, 앱 가입 페이지, 가입 신청 승인이 모두 여기를 거쳐 AppUser 로 등록된다.
 * - 가입 설정(AppJoinSetting): 가입 방식(초대만/승인/자유) + 새 사용자를 넣을 기본 사용자 그룹
 * - 사용자(APPLICATION_USER)로 새로 들어오면 기본 사용자 그룹에 넣는다. 관리자로 들어온 사람은 넣지 않는다
 */
@Service
@RequiredArgsConstructor
public class AppMembershipService {
    private final AppJoinSettingRepository appJoinSettingRepository;
    private final AppJoinRequestRepository appJoinRequestRepository;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupUserRepository userGroupUserRepository;
    private final UserRepository userRepository;

    // ---- 가입 설정 ----

    @Transactional(readOnly = true)
    public AppJoinSettingResponse getSetting(String applicationId) {
        Optional<AppJoinSetting> setting = appJoinSettingRepository.findByApplicationId(applicationId);
        Long defaultUserGroupId = setting.map(AppJoinSetting::getDefaultUserGroupId).orElse(null);

        return AppJoinSettingResponse.builder()
                .joinPolicy(policyOf(applicationId).name())
                .defaultUserGroupId(userGroupOf(applicationId, defaultUserGroupId).map(UserGroup::getId).orElse(null))
                .build();
    }

    @Transactional(rollbackFor = JoinException.class)
    public AppJoinSettingResponse saveSetting(String applicationId, String joinPolicy, Long defaultUserGroupId) throws JoinException {
        AppJoinPolicy policy = AppJoinPolicy.of(joinPolicy);
        if(policy == null) {
            throw new JoinException("알 수 없는 가입 방식입니다.");
        }
        if(defaultUserGroupId != null && userGroupOf(applicationId, defaultUserGroupId).isEmpty()) {
            throw new JoinException("이 애플리케이션의 사용자 그룹이 아닙니다.");
        }

        AppJoinSetting setting = appJoinSettingRepository.findByApplicationId(applicationId)
                .orElseGet(() -> AppJoinSetting.builder().applicationId(applicationId).joinPolicy(policy.name()).build());
        setting.update(policy, defaultUserGroupId);
        appJoinSettingRepository.save(setting);

        return getSetting(applicationId);
    }

    @Transactional(readOnly = true)
    public AppJoinPolicy policyOf(String applicationId) {
        return appJoinSettingRepository.findByApplicationId(applicationId)
                .map(AppJoinSetting::policy)
                .orElse(AppJoinPolicy.INVITE_ONLY);
    }

    // ---- 가입 ----

    @Transactional(readOnly = true)
    public AppJoinStatus statusOf(String applicationId, Long userId) {
        if(appUserRepository.findByApplicationIdAndUserId(applicationId, userId).isPresent()) {
            return AppJoinStatus.MEMBER;
        }
        if(appJoinRequestRepository.findByApplicationIdAndUserId(applicationId, userId).isPresent()) {
            return AppJoinStatus.PENDING;
        }
        return AppJoinStatus.NONE;
    }

    /**
     * 초대/승인으로 애플리케이션에 들어온다. 관리자를 사용자로 낮추지는 않고, 사용자로 새로 들어오면 기본 사용자 그룹에 넣는다.
     * 기다리던 가입 신청이 있으면 지운다.
     */
    @Transactional
    public AppUser join(String applicationId, Long userId, String authority) {
        return join(applicationId, userId, authority, null);
    }

    /** 회사를 지정한 초대로 들어온다. companyCode 가 비어 있으면 회사 미지정 */
    @Transactional
    public AppUser join(String applicationId, Long userId, String authority, String companyCode) {
        boolean wasMember = appUserRepository.findByApplicationIdAndUserId(applicationId, userId).isPresent();

        AppUser appUser = appUserService.joinApplication(applicationId, userId, authority, companyCode);

        if(!wasMember && AppUser.AUTHORITY_APPLICATION_USER.equals(appUser.getAuthority())) {
            addToDefaultUserGroup(applicationId, userId);
        }
        appJoinRequestRepository.findByApplicationIdAndUserId(applicationId, userId)
                .ifPresent(appJoinRequestRepository::delete);

        return appUser;
    }

    /**
     * 앱 가입 페이지에서 가입한다 (가입 방식에 따라).
     * 자유 가입이면 바로 사용자가 되고, 승인 가입이면 가입 신청을 남긴다. 초대만 받는 애플리케이션이면 거절한다.
     * @return 가입 후 상태 (MEMBER / PENDING)
     */
    @Transactional(rollbackFor = JoinException.class)
    public AppJoinStatus applyForApplication(String applicationId, Long userId) throws JoinException {
        AppJoinStatus status = statusOf(applicationId, userId);
        if(status != AppJoinStatus.NONE) {
            return status;
        }

        switch (policyOf(applicationId)) {
            case OPEN:
                join(applicationId, userId, AppUser.AUTHORITY_APPLICATION_USER);
                return AppJoinStatus.MEMBER;

            case APPROVAL:
                appJoinRequestRepository.save(AppJoinRequest.builder()
                        .applicationId(applicationId)
                        .userId(userId)
                        .requestedAt(LocalDateTime.now())
                        .build());
                return AppJoinStatus.PENDING;

            default:
                throw new JoinException(INVITE_ONLY_MESSAGE);
        }
    }

    static final String INVITE_ONLY_MESSAGE = "이 애플리케이션은 초대받은 사람만 가입할 수 있습니다. 애플리케이션 관리자에게 초대를 요청해주세요.";

    // ---- 가입 신청 (승인 대기) ----

    @Transactional(readOnly = true)
    public List<AppJoinRequestResponse> getRequests(String applicationId) {
        List<AppJoinRequest> requests = appJoinRequestRepository.findByApplicationIdOrderByRequestedAtAsc(applicationId);
        Map<Long, User> users = userRepository.findAllById(requests.stream().map(AppJoinRequest::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return requests.stream()
                .map(request -> AppJoinRequestResponse.of(request, users.get(request.getUserId())))
                .toList();
    }

    @Transactional(rollbackFor = JoinException.class)
    public void approve(String applicationId, Long requestId) throws JoinException {
        AppJoinRequest request = requestOf(applicationId, requestId);
        if(userRepository.findById(request.getUserId()).isEmpty()) {
            appJoinRequestRepository.delete(request);
            throw new JoinException("탈퇴했거나 삭제된 사용자입니다.");
        }

        join(applicationId, request.getUserId(), AppUser.AUTHORITY_APPLICATION_USER);
    }

    @Transactional(rollbackFor = JoinException.class)
    public void reject(String applicationId, Long requestId) throws JoinException {
        appJoinRequestRepository.delete(requestOf(applicationId, requestId));
    }

    private AppJoinRequest requestOf(String applicationId, Long requestId) throws JoinException {
        return appJoinRequestRepository.findById(requestId)
                .filter(found -> applicationId.equals(found.getApplicationId()))
                .orElseThrow(() -> new JoinException("가입 신청을 찾을 수 없습니다."));
    }

    // ---- 기본 사용자 그룹 ----

    private void addToDefaultUserGroup(String applicationId, Long userId) {
        Long groupId = appJoinSettingRepository.findByApplicationId(applicationId)
                .map(AppJoinSetting::getDefaultUserGroupId)
                .orElse(null);

        // 설정한 그룹이 지워졌으면 그룹에 넣지 않는다
        Optional<UserGroup> group = userGroupOf(applicationId, groupId);
        Optional<User> user = userRepository.findById(userId);
        if(group.isEmpty() || user.isEmpty()) {
            return;
        }

        boolean alreadyIn = userGroupUserRepository.findByUserGroupId(group.get().getId()).stream()
                .anyMatch(member -> member.getUser() != null && userId.equals(member.getUser().getId()));
        if(!alreadyIn) {
            userGroupUserRepository.save(UserGroupUser.builder().userGroup(group.get()).user(user.get()).build());
        }
    }

    private Optional<UserGroup> userGroupOf(String applicationId, Long groupId) {
        if(groupId == null) {
            return Optional.empty();
        }
        return userGroupRepository.findById(groupId)
                .filter(group -> applicationId.equals(group.getApplicationId()));
    }
}
