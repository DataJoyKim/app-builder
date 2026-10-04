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

import static org.hamcrest.Matchers.*;
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
        appAdmin = user("home-admin", "관리자", null);
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
    void 애플리케이션_관리자는_소유_애플리케이션_관리_화면으로_보낸다() throws Exception {
        mockMvc.perform(get("/").cookie(tokenOf(appAdmin)))
                .andExpect(redirectedUrl("/applications/manage"));

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
                .andExpect(content().string(not(containsString("href=\"/old\""))));
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
    void 어느_애플리케이션에도_없는_사용자는_애플리케이션_만들기_화면으로_보낸다() throws Exception {
        mockMvc.perform(get("/").cookie(tokenOf(newcomer)))
                .andExpect(redirectedUrl("/applications/manage"));
    }

    // 공개 가입으로 누구나 계정을 만들 수 있으므로 애플리케이션 생성은 로그인한 사용자 누구나 할 수 있다
    @Test
    void 일반_사용자도_자기_애플리케이션을_만들_수_있다() throws Exception {
        mockMvc.perform(get("/applications/manage").cookie(tokenOf(member)))
                .andExpect(status().isOk())
                .andExpect(view().name("home/application-manage"));

        mockMvc.perform(get("/api/my-applications").cookie(tokenOf(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications", empty()));

        mockMvc.perform(post("/api/my-applications").cookie(tokenOf(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicationId\":\"member-app\",\"name\":\"내 앱\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/my-applications").cookie(tokenOf(member)))
                .andExpect(jsonPath("$.applications[*].applicationId", contains("member-app")));
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
