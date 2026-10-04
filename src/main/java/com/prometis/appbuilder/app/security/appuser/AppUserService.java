package com.prometis.appbuilder.app.security.appuser;

import com.prometis.appbuilder.app.security.usergroup.UserGroupUserRepository;
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
    private final UserGroupUserRepository userGroupUserRepository;

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

    /**
     * 초대/가입으로 애플리케이션에 들어온 사용자를 등록한다.
     * 관리자로 들어오면 관리자로 지정하고(이미 사용자면 올린다), 사용자로 들어오면 아직 없을 때만 사용자로 등록한다.
     * 이미 관리자인 사람을 사용자로 낮추지는 않는다.
     */
    @Transactional
    public AppUser joinApplication(String applicationId, Long userId, String authority) {
        if(AppUser.AUTHORITY_APPLICATION_ADMIN.equals(authority)) {
            return grantApplicationAdmin(applicationId, userId);
        }

        return appUserRepository.findByApplicationIdAndUserId(applicationId, userId)
                .orElseGet(() -> appUserRepository.save(AppUser.builder()
                        .applicationId(applicationId)
                        .userId(userId)
                        .authority(AppUser.AUTHORITY_APPLICATION_USER)
                        .build()));
    }

    /**
     * 애플리케이션 사용자의 권한을 바꾼다. 마지막 관리자를 사용자로 낮추면 애플리케이션을 관리할 사람이 없어지므로 막는다.
     * 본인의 권한은 바꿀 수 없다. (스스로 낮추면 그 즉시 콘솔에서 쫓겨난다)
     * @param requesterUserId 요청한(로그인한) 사용자의 userId
     */
    @Transactional
    public AppUser changeAuthority(String applicationId, Long appUserId, String authority, Long requesterUserId) throws AppUserManageException {
        if(!AppUser.AUTHORITY_APPLICATION_ADMIN.equals(authority) && !AppUser.AUTHORITY_APPLICATION_USER.equals(authority)) {
            throw new AppUserManageException("변경할 수 없는 권한입니다.");
        }

        AppUser appUser = findAppUser(applicationId, appUserId);
        checkNotSelf(appUser, requesterUserId, "본인의 권한은 변경할 수 없습니다.");
        if(isAdmin(appUser) && !AppUser.AUTHORITY_APPLICATION_ADMIN.equals(authority)) {
            checkNotLastAdmin(applicationId, "마지막 관리자의 권한은 변경할 수 없습니다. 다른 사용자를 관리자로 지정한 뒤 변경해주세요.");
        }

        appUser.changeAuthority(authority);
        return appUser;
    }

    /**
     * 애플리케이션 사용자에서 삭제한다. 마지막 관리자는 삭제할 수 없다.
     * 이 애플리케이션의 사용자 그룹 소속도 함께 지운다. 남겨두면 다시 등록했을 때 예전 그룹 권한이 그대로 살아난다.
     * 다른 애플리케이션의 그룹 소속은 건드리지 않는다. 본인은 삭제할 수 없다.
     * @param requesterUserId 요청한(로그인한) 사용자의 userId
     */
    @Transactional
    public void deleteAppUser(String applicationId, Long appUserId, Long requesterUserId) throws AppUserManageException {
        AppUser appUser = findAppUser(applicationId, appUserId);
        checkNotSelf(appUser, requesterUserId, "본인은 삭제할 수 없습니다.");
        if(isAdmin(appUser)) {
            checkNotLastAdmin(applicationId, "마지막 관리자는 삭제할 수 없습니다. 다른 사용자를 관리자로 지정한 뒤 삭제해주세요.");
        }

        userGroupUserRepository.deleteAll(
                userGroupUserRepository.findByUserIdAndUserGroupApplicationId(appUser.getUserId(), applicationId));
        appUserRepository.delete(appUser);
    }

    // 다른 애플리케이션의 행은 id 가 맞아도 건드리지 않는다
    private AppUser findAppUser(String applicationId, Long appUserId) throws AppUserManageException {
        return appUserRepository.findById(appUserId)
                .filter(appUser -> applicationId.equals(appUser.getApplicationId()))
                .orElseThrow(() -> new AppUserManageException("등록된 사용자가 아닙니다."));
    }

    private void checkNotSelf(AppUser appUser, Long requesterUserId, String message) throws AppUserManageException {
        // 요청자를 모르면 본인인지 가릴 수 없으므로 허용하지 않는다
        if(requesterUserId == null) {
            throw new AppUserManageException("로그인이 필요합니다.");
        }
        if(requesterUserId.equals(appUser.getUserId())) {
            throw new AppUserManageException(message);
        }
    }

    private boolean isAdmin(AppUser appUser) {
        return AppUser.AUTHORITY_APPLICATION_ADMIN.equals(appUser.getAuthority());
    }

    private void checkNotLastAdmin(String applicationId, String message) throws AppUserManageException {
        if(getApplicationAdmins(applicationId).size() <= 1) {
            throw new AppUserManageException(message);
        }
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
