package com.prometis.appbuilder.console.app.dashboard;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.app.security.session.AppSession;
import com.prometis.appbuilder.app.security.session.AppSessionRepository;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import com.prometis.appbuilder.platform.join.AppInvitation;
import com.prometis.appbuilder.platform.join.AppInvitationRepository;
import com.prometis.appbuilder.platform.join.AppJoinRequest;
import com.prometis.appbuilder.platform.join.AppJoinRequestRepository;
import com.prometis.appbuilder.platform.join.AppJoinSettingResponse;
import com.prometis.appbuilder.platform.join.AppMembershipService;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 콘솔 첫 화면 사용현황 지표가 지금 있는 데이터로 맞게 세어지는지 본다. 다른 애플리케이션 데이터는 섞이지 않아야 한다.
 */
@DataJpaTest
@Import(AppDashboardService.class)
class AppDashboardServiceTest {
    private static final String APP = "dash";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 12, 0);

    @Autowired
    private AppDashboardService service;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private AppSessionRepository appSessionRepository;
    @Autowired
    private AppInvitationRepository appInvitationRepository;
    @Autowired
    private AppJoinRequestRepository appJoinRequestRepository;
    @Autowired
    private WorkflowRepository workflowRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;
    @MockBean
    private AppMembershipService appMembershipService;

    @BeforeEach
    void setUp() {
        when(appMembershipService.getSetting(anyString()))
                .thenReturn(AppJoinSettingResponse.builder().joinPolicy("APPROVAL").build());
    }

    private long user(String name) {
        return userRepository.save(User.builder().loginId(name).userName(name).password("pw").email(name + "@x.com").build()).getId();
    }

    private void appUser(String applicationId, long userId, String authority, String companyCode,
                         LocalDateTime joinedAt, LocalDateTime lastAccessAt) {
        appUserRepository.save(AppUser.builder().applicationId(applicationId).userId(userId).authority(authority)
                .companyCode(companyCode).joinedAt(joinedAt).lastAccessAt(lastAccessAt).build());
    }

    private void invitation(String applicationId, String email, LocalDateTime expiresAt, LocalDateTime acceptedAt) {
        appInvitationRepository.save(AppInvitation.builder()
                .token(email + applicationId)
                .applicationId(applicationId)
                .email(email)
                .authority(AppUser.AUTHORITY_APPLICATION_USER)
                .createdAt(NOW.minusDays(60))
                .expiresAt(expiresAt)
                .acceptedAt(acceptedAt)
                .build());
    }

    @Test
    void 사용자_초대_가입신청_구성요소를_센다() {
        long admin = user("admin");
        long recent = user("recent");
        long newest = user("newest");
        long idle = user("idle");
        long other = user("other");

        // admin 은 접속 시각을 기록하기 전에 접속해서 세션만 있다.
        appUser(APP, admin, AppUser.AUTHORITY_APPLICATION_ADMIN, "C1", NOW.minusDays(100), null);
        appUser(APP, recent, AppUser.AUTHORITY_APPLICATION_USER, null, NOW.minusDays(10), NOW.minusDays(2));
        appUser(APP, newest, AppUser.AUTHORITY_APPLICATION_USER, "C1", NOW.minusDays(1), NOW.minusDays(20));
        appUser(APP, idle, AppUser.AUTHORITY_APPLICATION_USER, null, NOW.minusDays(50), null);
        appUser("other", other, AppUser.AUTHORITY_APPLICATION_USER, null, NOW, NOW);

        // 9999 는 이미 빠진 사용자의 세션이라 세지 않는다.
        appSessionRepository.save(AppSession.builder().applicationId(APP).userId(admin).createAt(NOW).build());
        appSessionRepository.save(AppSession.builder().applicationId(APP).userId(9999L).createAt(NOW).build());

        invitation(APP, "pending@x.com", NOW.plusDays(1), null);
        invitation(APP, "expired@x.com", NOW.minusDays(1), null);
        invitation(APP, "old@x.com", NOW.minusDays(40), NOW.minusDays(45));
        invitation(APP, "new@x.com", NOW.plusDays(1), NOW.minusDays(2));
        invitation("other", "other@x.com", NOW.plusDays(1), null);

        appJoinRequestRepository.save(AppJoinRequest.builder().applicationId(APP).userId(5L).requestedAt(NOW).build());

        workflowRepository.save(Workflow.builder().applicationId(APP).workflowCode("WF1").displayName("w").useAuthValidation(true).build());

        AppDashboardResponse result = service.dashboard(APP, NOW);

        // total, admins, members, loggedIn, neverLoggedIn, withoutCompany, active7, active30, joined30
        assertEquals(new AppDashboardResponse.Users(4, 1, 3, 3, 1, 2, 1, 2, 2), result.users());

        AppDashboardResponse.Membership membership = result.membership();
        assertEquals("APPROVAL", membership.joinPolicy());
        assertEquals(1, membership.pendingRequests());
        assertEquals(1, membership.invitationsPending());
        assertEquals(1, membership.invitationsExpired());
        assertEquals(2, membership.invitationsAccepted());
        assertEquals(1, membership.acceptedLast30Days());
        assertEquals(List.of("newest", "recent", "idle", "admin"),
                membership.recentJoins().stream().map(AppDashboardResponse.RecentJoin::userName).toList());
        assertEquals("newest@x.com", membership.recentJoins().get(0).email());

        assertEquals(1, result.resources().workflows());
        assertEquals(0, result.resources().restApis());
        assertEquals(0, result.organization().companies());
    }

    @Test
    void 가입하면_가입시각이_채워진다() {
        AppUser saved = appUserRepository.save(AppUser.builder().applicationId(APP).userId(user("joined")).authority(AppUser.AUTHORITY_APPLICATION_USER).build());
        assertNotNull(saved.getJoinedAt());
        assertNotNull(userRepository.findByLoginId("joined").orElseThrow().getCreatedAt());
    }

    @Test
    void 접속시각은_간격이_지났을때만_다시_쓰고_로그인시각은_매번_쓴다() {
        long userId = user("access");
        appUser(APP, userId, AppUser.AUTHORITY_APPLICATION_USER, null, NOW.minusDays(1), null);

        assertEquals(1, appUserRepository.recordAccess(APP, userId, NOW, NOW.minusMinutes(10)), "처음 접속");
        assertEquals(0, appUserRepository.recordAccess(APP, userId, NOW.plusMinutes(5), NOW.plusMinutes(5).minusMinutes(10)), "간격 안");
        assertEquals(1, appUserRepository.recordAccess(APP, userId, NOW.plusMinutes(11), NOW.plusMinutes(11).minusMinutes(10)), "간격 지남");
        assertEquals(0, appUserRepository.recordAccess("other", userId, NOW, NOW), "다른 애플리케이션 사용자가 아님");

        assertEquals(1, userRepository.recordLogin(userId, NOW));

        entityManager.clear();
        assertEquals(NOW.plusMinutes(11), appUserRepository.findByApplicationIdAndUserId(APP, userId).orElseThrow().getLastAccessAt());
        assertEquals(NOW, userRepository.findById(userId).orElseThrow().getLastLoginAt());
    }
}
