package com.prometis.appbuilder.console.app.dashboard;

import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMetaRepository;
import com.prometis.appbuilder.app.restapi.RestApiRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerJobRepository;
import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.app.security.company.CompanyRepository;
import com.prometis.appbuilder.app.security.session.AppSession;
import com.prometis.appbuilder.app.security.session.AppSessionRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import com.prometis.appbuilder.app.view.MenuRepository;
import com.prometis.appbuilder.app.view.ViewObjectRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import com.prometis.appbuilder.platform.join.AppInvitation;
import com.prometis.appbuilder.platform.join.AppInvitationRepository;
import com.prometis.appbuilder.platform.join.AppJoinRequestRepository;
import com.prometis.appbuilder.platform.join.AppMembershipService;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 콘솔 첫 화면의 애플리케이션 사용현황. 사용자/초대/가입신청/구성요소 수를 지금 있는 데이터로 센다.
 * 가입 시각(AppUser.joinedAt)과 마지막 접속 시각(AppUser.lastAccessAt)은 이 값을 저장하기 시작한 뒤의 사용자/접속에만 있다.
 */
@Service
@RequiredArgsConstructor
public class AppDashboardService {
    static final int RECENT_JOIN_LIMIT = 5;
    static final int RECENT_DAYS = 30;
    static final int ACTIVE_WEEK_DAYS = 7;

    private final AppUserRepository appUserRepository;
    private final UserRepository userRepository;
    private final AppSessionRepository appSessionRepository;
    private final AppInvitationRepository appInvitationRepository;
    private final AppJoinRequestRepository appJoinRequestRepository;
    private final AppMembershipService appMembershipService;
    private final CompanyRepository companyRepository;
    private final UserGroupRepository userGroupRepository;
    private final WorkflowRepository workflowRepository;
    private final ViewObjectRepository viewObjectRepository;
    private final MenuRepository menuRepository;
    private final RestApiRepository restApiRepository;
    private final SchedulerJobRepository schedulerJobRepository;
    private final DataSourceDatabaseMetaRepository databaseRepository;

    @Transactional(readOnly = true)
    public AppDashboardResponse dashboard(String applicationId) {
        return dashboard(applicationId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public AppDashboardResponse dashboard(String applicationId, LocalDateTime now) {
        List<AppUser> appUsers = appUserRepository.findByApplicationId(applicationId);

        return new AppDashboardResponse(
                users(appUsers, applicationId, now),
                membership(appUsers, applicationId, now),
                new AppDashboardResponse.Organization(
                        companyRepository.countByApplicationId(applicationId),
                        userGroupRepository.countByApplicationId(applicationId)),
                new AppDashboardResponse.Resources(
                        workflowRepository.countByApplicationId(applicationId),
                        viewObjectRepository.countByApplicationId(applicationId),
                        menuRepository.countByApplicationId(applicationId),
                        restApiRepository.countByApplicationId(applicationId),
                        schedulerJobRepository.countByApplicationId(applicationId),
                        databaseRepository.countByApplicationId(applicationId))
        );
    }

    private AppDashboardResponse.Users users(List<AppUser> appUsers, String applicationId, LocalDateTime now) {
        long admins = appUsers.stream().filter(u -> AppUser.AUTHORITY_APPLICATION_ADMIN.equals(u.getAuthority())).count();
        long members = appUsers.stream().filter(u -> AppUser.AUTHORITY_APPLICATION_USER.equals(u.getAuthority())).count();
        long withoutCompany = appUsers.stream().filter(u -> !u.hasCompany()).count();

        // 세션은 탈퇴/제외된 사용자 것이 남아 있을 수 있어 지금 사용자로만 센다.
        // 접속 시각을 기록하기 전에 접속한 사용자는 세션만 있으므로 둘 중 하나라도 있으면 접속한 적 있는 사용자다.
        Set<Long> sessionUserIds = appSessionRepository.findByApplicationId(applicationId).stream()
                .map(AppSession::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        long loggedIn = appUsers.stream()
                .filter(u -> u.getLastAccessAt() != null || sessionUserIds.contains(u.getUserId()))
                .count();

        long activeWeek = countSince(appUsers, AppUser::getLastAccessAt, now.minusDays(ACTIVE_WEEK_DAYS));
        long activeMonth = countSince(appUsers, AppUser::getLastAccessAt, now.minusDays(RECENT_DAYS));
        long joinedMonth = countSince(appUsers, AppUser::getJoinedAt, now.minusDays(RECENT_DAYS));

        return new AppDashboardResponse.Users(appUsers.size(), admins, members, loggedIn, appUsers.size() - loggedIn, withoutCompany,
                activeWeek, activeMonth, joinedMonth);
    }

    private static long countSince(List<AppUser> appUsers, Function<AppUser, LocalDateTime> time, LocalDateTime since) {
        return appUsers.stream().map(time).filter(t -> t != null && !t.isBefore(since)).count();
    }

    private AppDashboardResponse.Membership membership(List<AppUser> appUsers, String applicationId, LocalDateTime now) {
        List<AppInvitation> invitations = appInvitationRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId);

        long pending = invitations.stream().filter(i -> !i.isAccepted() && !i.isExpired(now)).count();
        long expired = invitations.stream().filter(i -> !i.isAccepted() && i.isExpired(now)).count();
        List<AppInvitation> accepted = invitations.stream()
                .filter(AppInvitation::isAccepted)
                .sorted(Comparator.comparing(AppInvitation::getAcceptedAt).reversed())
                .toList();

        LocalDateTime since = now.minusDays(RECENT_DAYS);
        long acceptedRecently = accepted.stream().filter(i -> !i.getAcceptedAt().isBefore(since)).count();

        return new AppDashboardResponse.Membership(
                appMembershipService.getSetting(applicationId).getJoinPolicy(),
                appJoinRequestRepository.countByApplicationId(applicationId),
                pending, expired, accepted.size(), acceptedRecently, recentJoins(appUsers));
    }

    // 가입 경로(초대/가입 페이지/승인/앱 생성)와 상관없이 AppUser 가 생긴 시각 순으로 보여준다.
    private List<AppDashboardResponse.RecentJoin> recentJoins(List<AppUser> appUsers) {
        List<AppUser> recent = appUsers.stream()
                .filter(u -> u.getJoinedAt() != null)
                .sorted(Comparator.comparing(AppUser::getJoinedAt).reversed())
                .limit(RECENT_JOIN_LIMIT)
                .toList();

        Map<Long, User> users = userRepository.findAllById(recent.stream().map(AppUser::getUserId).filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return recent.stream()
                .map(u -> {
                    User user = users.get(u.getUserId());
                    return new AppDashboardResponse.RecentJoin(
                            user == null ? null : user.getUserName(),
                            user == null ? null : user.getEmail(),
                            u.getAuthority(),
                            u.getJoinedAt());
                })
                .toList();
    }
}
