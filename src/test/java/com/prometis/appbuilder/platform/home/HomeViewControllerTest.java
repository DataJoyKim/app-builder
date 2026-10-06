package com.prometis.appbuilder.platform.home;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.application.ApplicationStatus;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import com.prometis.appbuilder.security.token.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 루트(/) 진입 시 로그인 여부/사용자 유형에 따라 첫 화면을 나누고,
 * 일반 사용자 애플리케이션 선택 화면과 애플리케이션 관리자 화면이 맞는 사용자에게만 열리는지 확인한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:home;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc
class HomeViewControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtProvider jwtProvider;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    ApplicationRepository applicationRepository;

    private User platformAdmin;
    private User appAdmin;
    private User member;
    private User newcomer;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();

        application("ehr", "인사관리", ApplicationStatus.ACTIVE);
        application("crm", "고객관리", ApplicationStatus.ACTIVE);
        application("old", "구시스템", ApplicationStatus.INACTIVE);

        platformAdmin = user("home-platform", "플랫폼", User.AUTHORITY_PLATFORM_ADMIN);
        appAdmin = user("home-admin", "관리자", User.AUTHORITY_APPLICATION_ADMIN);
        member = user("home-member", "일반", null);
        newcomer = user("home-newcomer", "신규", null);

        appUser("ehr", appAdmin, AppUser.AUTHORITY_APPLICATION_ADMIN);
        appUser("ehr", member, AppUser.AUTHORITY_APPLICATION_USER);
        appUser("crm", member, AppUser.AUTHORITY_APPLICATION_USER);
        appUser("old", member, AppUser.AUTHORITY_APPLICATION_USER);
    }

    private void application(String applicationId, String name, ApplicationStatus status) {
        if(applicationRepository.findByApplicationId(applicationId).isEmpty()) {
            applicationRepository.save(Application.builder()
                    .applicationId(applicationId)
                    .name(name)
                    .status(status.name())
                    .build());
        }
    }

    private User user(String loginId, String userName, String authority) {
        return userRepository.findByLoginId(loginId).orElseGet(() -> userRepository.save(User.builder()
                .loginId(loginId)
                .userName(userName)
                .password("x")
                .email(loginId + "@test.com")
                .authority(authority)
                .build()));
    }

    private void appUser(String applicationId, User user, String authority) {
        appUserRepository.save(AppUser.builder()
                .applicationId(applicationId)
                .userId(user.getId())
                .authority(authority)
                .build());
    }

    private Cookie tokenOf(User user) {
        return new Cookie("accessToken", jwtProvider.generateAccessToken(user.getId()));
    }

    @Test
    void 로그인하지_않았으면_로그인_화면으로_보낸다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void 플랫폼관리자는_플랫폼_콘솔로_보낸다() throws Exception {
        mockMvc.perform(get("/").cookie(tokenOf(platformAdmin)))
                .andExpect(redirectedUrl("/console"));
    }

    @Test
    void 애플리케이션_관리자도_애플리케이션_선택_화면으로_보낸다() throws Exception {
        mockMvc.perform(get("/").cookie(tokenOf(appAdmin)))
                .andExpect(redirectedUrl("/applications"));

        // 생성 화면은 첫 화면이 아닐 뿐, 주소로 직접 들어갈 수 있다

        mockMvc.perform(get("/applications/manage").cookie(tokenOf(appAdmin)))
                .andExpect(status().isOk())
                .andExpect(view().name("home/application-manage"));
    }

    @Test
    void 일반_사용자는_애플리케이션_선택_화면으로_보낸다() throws Exception {
        mockMvc.perform(get("/").cookie(tokenOf(member)))
                .andExpect(redirectedUrl("/applications"));
    }

    @Test
    void 선택_화면에는_가입된_사용중_애플리케이션만_나온다() throws Exception {
        mockMvc.perform(get("/applications").cookie(tokenOf(member)))
                .andExpect(status().isOk())
                .andExpect(view().name("home/application-select"))
                .andExpect(model().attribute("applications", hasSize(2)))
                .andExpect(model().attribute("applications", contains(
                        hasProperty("applicationId", is("crm")),
                        hasProperty("applicationId", is("ehr"))
                )))
                .andExpect(content().string(containsString("href=\"/ehr\"")))
                .andExpect(content().string(not(containsString("href=\"/old\""))))
                // 선택 화면에서는 애플리케이션을 만들지 않는다
                .andExpect(content().string(not(containsString("/applications/manage"))));
    }

    @Test
    void 가입된_애플리케이션이_없어도_선택_화면이_열린다() throws Exception {
        mockMvc.perform(get("/applications").cookie(tokenOf(newcomer)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("applications", empty()))
                .andExpect(content().string(containsString("가입된 애플리케이션이 없습니다")));
    }

    @Test
    void 로그인하지_않고_화면에_오면_돌아올_주소를_달아_로그인으로_보낸다() throws Exception {
        mockMvc.perform(get("/applications"))
                .andExpect(redirectedUrl("/login?returnUrl=%2Fapplications"));
        mockMvc.perform(get("/applications/manage"))
                .andExpect(redirectedUrl("/login?returnUrl=%2Fapplications%2Fmanage"));
    }

    @Test
    void 어느_애플리케이션에도_없는_사용자도_애플리케이션_선택_화면으로_보낸다() throws Exception {
        mockMvc.perform(get("/").cookie(tokenOf(newcomer)))
                .andExpect(redirectedUrl("/applications"));
    }

    // 애플리케이션 생성은 users.authority 가 APPLICATION_ADMIN 인 사용자만 할 수 있다
    @Test
    void 애플리케이션_관리자만_애플리케이션_관리_화면에서_애플리케이션을_만든다() throws Exception {
        mockMvc.perform(get("/applications/manage").cookie(tokenOf(appAdmin)))
                .andExpect(status().isOk())
                .andExpect(view().name("home/application-manage"));

        mockMvc.perform(post("/api/my-applications").cookie(tokenOf(appAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicationId\":\"admin-app\",\"name\":\"관리자 앱\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/my-applications").cookie(tokenOf(appAdmin)))
                .andExpect(jsonPath("$.applications[*].applicationId", hasItem("admin-app")));
    }

    @Test
    void 애플리케이션_관리자가_아니면_관리_화면과_생성_API를_쓸_수_없다() throws Exception {
        // AppUser 로는 관리자여도 users.authority 가 APPLICATION_ADMIN 이 아니면 안 된다
        User consoleOnlyAdmin = user("home-console-admin", "앱관리자", null);
        appUser("ehr", consoleOnlyAdmin, AppUser.AUTHORITY_APPLICATION_ADMIN);

        for(User user : List.of(member, newcomer, consoleOnlyAdmin)) {
            mockMvc.perform(get("/applications/manage").cookie(tokenOf(user)))
                    .andExpect(redirectedUrl("/applications"));

            mockMvc.perform(get("/api/my-applications").cookie(tokenOf(user)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/my-applications").cookie(tokenOf(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"applicationId\":\"denied-app\",\"name\":\"안 됨\"}"))
                    .andExpect(status().isForbidden());
        }

        assertTrue(applicationRepository.findByApplicationId("denied-app").isEmpty());
    }

    @Test
    void 선택_화면의_애플리케이션_관리_버튼은_애플리케이션_관리자에게만_보인다() throws Exception {
        mockMvc.perform(get("/applications").cookie(tokenOf(appAdmin)))
                .andExpect(model().attribute("canManageApplications", true))
                .andExpect(content().string(containsString("href=\"/applications/manage\"")));

        mockMvc.perform(get("/applications").cookie(tokenOf(member)))
                .andExpect(model().attribute("canManageApplications", false))
                .andExpect(content().string(not(containsString("/applications/manage"))));

        // 아직 애플리케이션이 없는 관리자(공개 가입 직후)에게도 보인다
        User freshAdmin = user("home-fresh-admin", "신규관리자", User.AUTHORITY_APPLICATION_ADMIN);
        mockMvc.perform(get("/applications").cookie(tokenOf(freshAdmin)))
                .andExpect(content().string(containsString("href=\"/applications/manage\"")));
    }

    @Test
    void 로그인하지_않으면_애플리케이션_API는_401() throws Exception {
        mockMvc.perform(get("/api/my-applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 애플리케이션_관리자는_소유_애플리케이션을_조회한다() throws Exception {
        mockMvc.perform(get("/api/my-applications").cookie(tokenOf(appAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications[*].applicationId", contains("ehr")));
    }

    @Test
    void 로그아웃하면_토큰_쿠키를_지운다() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(tokenOf(member)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }
}
