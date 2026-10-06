package com.prometis.appbuilder.platform.join;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUserRepository;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.application.ApplicationStatus;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import com.prometis.appbuilder.security.token.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 애플리케이션 가입 페이지(/{applicationId}/signup)와 콘솔의 가입 설정/가입 신청 승인.
 * 콘솔 보안 필터를 그대로 거친다. 메일은 JoinMailSender 를 목으로 바꾼다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:app-signup;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false",
        "platform.join.resend-cooldown=0s"
})
@AutoConfigureMockMvc
class AppSignupTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtProvider jwtProvider;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    UserGroupUserRepository userGroupUserRepository;
    @Autowired
    AppJoinSettingRepository appJoinSettingRepository;
    @Autowired
    AppJoinRequestRepository appJoinRequestRepository;
    @Autowired
    JoinVerificationRepository joinVerificationRepository;
    @Autowired
    AppMembershipService appMembershipService;
    @MockBean
    JoinMailSender joinMailSender;

    private User owner;
    private User existing;
    private UserGroup defaultGroup;

    @BeforeEach
    void setUp() throws Exception {
        joinVerificationRepository.deleteAll();
        appJoinRequestRepository.deleteAll();
        appJoinSettingRepository.deleteAll();
        appUserRepository.deleteAll();
        userGroupUserRepository.deleteAll();
        userGroupRepository.deleteAll();
        userRepository.findByLoginId("joiner").ifPresent(userRepository::delete);

        application("shop", "쇼핑몰");
        application("crm", "고객관리");

        owner = user("as-owner", "소유자", "owner@test.com");
        existing = user("as-existing", "기존사용자", "existing@test.com");
        appUserRepository.save(AppUser.builder().applicationId("shop").userId(owner.getId()).authority(AppUser.AUTHORITY_APPLICATION_ADMIN).build());

        defaultGroup = userGroupRepository.save(UserGroup.builder().applicationId("shop").companyCode("C001").code("MEMBER").name("회원").build());
    }

    private void application(String applicationId, String name) {
        if(applicationRepository.findByApplicationId(applicationId).isEmpty()) {
            applicationRepository.save(Application.builder().applicationId(applicationId).name(name).status(ApplicationStatus.ACTIVE.name()).build());
        }
    }

    private User user(String loginId, String userName, String email) {
        return userRepository.findByLoginId(loginId).orElseGet(() -> userRepository.save(User.builder()
                .loginId(loginId).userName(userName).password("x").email(email).build()));
    }

    private Cookie tokenOf(User user) {
        return new Cookie("accessToken", jwtProvider.generateAccessToken(user.getId()));
    }

    private void policy(AppJoinPolicy policy) throws Exception {
        appMembershipService.saveSetting("shop", policy.name(), defaultGroup.getId());
    }

    private boolean inDefaultGroup(Long userId) {
        return userGroupUserRepository.findByUserGroupId(defaultGroup.getId()).stream()
                .anyMatch(member -> member.getUser().getId().equals(userId));
    }

    private Map<String, String> joinInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("loginId", "joiner");
        info.put("userName", "가입자");
        info.put("email", "joiner@test.com");
        info.put("password", "pw1234!");
        info.put("checkPassword", "pw1234!");
        return info;
    }

    private ResultActions requestCode() throws Exception {
        return mockMvc.perform(post("/shop/api/signup/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinInfo())));
    }

    // 새 계정 인증코드 요청 → {verificationKey, 코드}
    private String[] requestAndCaptureCode() throws Exception {
        String body = requestCode()
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(joinMailSender, atLeastOnce()).sendVerificationCode(eq("joiner@test.com"), eq("쇼핑몰 가입"), code.capture(), any());

        return new String[]{objectMapper.readTree(body).get("verificationKey").asText(), code.getValue()};
    }

    private ResultActions verifyCode(String[] requested) throws Exception {
        return mockMvc.perform(post("/shop/api/signup/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("verificationKey", requested[0], "code", requested[1]))));
    }

    private ResultActions apply(User loginUser) throws Exception {
        var request = post("/shop/api/signup/apply").contentType(MediaType.APPLICATION_JSON).content("{}");
        if(loginUser != null) {
            request.cookie(tokenOf(loginUser));
        }
        return mockMvc.perform(request);
    }

    // ---- 가입 방식: 초대만 (기본) ----

    @Test
    void 가입_설정이_없으면_초대만_받고_가입_페이지는_안내만_보여준다() throws Exception {
        mockMvc.perform(get("/shop/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/app/join"))
                .andExpect(model().attribute("mode", "APP_SIGNUP"))
                .andExpect(model().attribute("joinPolicy", "INVITE_ONLY"))
                .andExpect(content().string(containsString("초대받은 사람만 가입할 수 있습니다")));

        requestCode()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("초대받은 사람만")));
        apply(existing)
                .andExpect(status().isBadRequest());

        verify(joinMailSender, never()).sendVerificationCode(anyString(), anyString(), anyString(), any());
        assertTrue(appUserRepository.findByApplicationIdAndUserId("shop", existing.getId()).isEmpty());
    }

    @Test
    void 없는_애플리케이션의_가입_페이지는_404() throws Exception {
        mockMvc.perform(get("/nope/signup"))
                .andExpect(view().name("/error/error404"));
    }

    // ---- 가입 방식: 자유 ----

    @Test
    void 자유_가입이면_새_계정이_바로_사용자가_되고_기본_그룹에_들어간다() throws Exception {
        policy(AppJoinPolicy.OPEN);

        verifyCode(requestAndCaptureCode())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MEMBER"))
                .andExpect(jsonPath("$.redirectUrl").value("/shop"))
                .andExpect(cookie().exists("accessToken"));

        User joiner = userRepository.findByLoginId("joiner").orElseThrow();
        assertEquals(AppUser.AUTHORITY_APPLICATION_USER,
                appUserRepository.findByApplicationIdAndUserId("shop", joiner.getId()).orElseThrow().getAuthority());
        assertTrue(inDefaultGroup(joiner.getId()));
    }

    @Test
    void 자유_가입이면_기존_계정은_로그인해서_바로_가입한다() throws Exception {
        policy(AppJoinPolicy.OPEN);

        mockMvc.perform(get("/shop/signup").cookie(tokenOf(existing)))
                .andExpect(model().attribute("appJoinStatus", "NONE"));

        apply(existing)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MEMBER"));

        assertTrue(appUserRepository.findByApplicationIdAndUserId("shop", existing.getId()).isPresent());
        assertTrue(inDefaultGroup(existing.getId()));

        // 다시 가입해도 그대로
        apply(existing)
                .andExpect(jsonPath("$.status").value("MEMBER"));
        assertEquals(1, userGroupUserRepository.findByUserGroupId(defaultGroup.getId()).size());
        mockMvc.perform(get("/shop/signup").cookie(tokenOf(existing)))
                .andExpect(model().attribute("appJoinStatus", "MEMBER"));
    }

    @Test
    void 이미_관리자인_사람이_가입해도_권한은_그대로다() throws Exception {
        policy(AppJoinPolicy.OPEN);

        apply(owner)
                .andExpect(jsonPath("$.status").value("MEMBER"));

        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN,
                appUserRepository.findByApplicationIdAndUserId("shop", owner.getId()).orElseThrow().getAuthority());
    }

    @Test
    void 로그인하지_않고_기존_계정_가입을_하면_401() throws Exception {
        policy(AppJoinPolicy.OPEN);

        apply(null).andExpect(status().isUnauthorized());
    }

    // ---- 가입 방식: 신청 후 승인 ----

    @Test
    void 승인_가입이면_신청이_남고_관리자가_승인하면_사용자가_된다() throws Exception {
        policy(AppJoinPolicy.APPROVAL);

        verifyCode(requestAndCaptureCode())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.redirectUrl").value("/shop/signup"));

        User joiner = userRepository.findByLoginId("joiner").orElseThrow();
        assertTrue(appUserRepository.findByApplicationIdAndUserId("shop", joiner.getId()).isEmpty());
        mockMvc.perform(get("/shop/signup").cookie(tokenOf(joiner)))
                .andExpect(model().attribute("appJoinStatus", "PENDING"));

        String requests = mockMvc.perform(get("/shop/console/api/app-join/request").cookie(tokenOf(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loginId").value("joiner"))
                .andReturn().getResponse().getContentAsString();
        long requestId = objectMapper.readTree(requests).get(0).get("id").asLong();

        mockMvc.perform(post("/shop/console/api/app-join/request/" + requestId + "/approve").cookie(tokenOf(owner)))
                .andExpect(status().isOk());

        assertEquals(AppUser.AUTHORITY_APPLICATION_USER,
                appUserRepository.findByApplicationIdAndUserId("shop", joiner.getId()).orElseThrow().getAuthority());
        assertTrue(inDefaultGroup(joiner.getId()));
        assertTrue(appJoinRequestRepository.findAll().isEmpty());
    }

    @Test
    void 거절하면_신청이_지워지고_다시_신청할_수_있다() throws Exception {
        policy(AppJoinPolicy.APPROVAL);

        apply(existing).andExpect(jsonPath("$.status").value("PENDING"));
        // 신청 중에 다시 눌러도 신청은 하나다
        apply(existing).andExpect(jsonPath("$.status").value("PENDING"));
        assertEquals(1, appJoinRequestRepository.findAll().size());

        Long requestId = appJoinRequestRepository.findAll().get(0).getId();
        mockMvc.perform(delete("/shop/console/api/app-join/request/" + requestId).cookie(tokenOf(owner)))
                .andExpect(status().isOk());

        assertTrue(appJoinRequestRepository.findAll().isEmpty());
        assertTrue(appUserRepository.findByApplicationIdAndUserId("shop", existing.getId()).isEmpty());

        apply(existing).andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 초대로_들어오면_기다리던_신청은_지운다() throws Exception {
        policy(AppJoinPolicy.APPROVAL);
        apply(existing).andExpect(jsonPath("$.status").value("PENDING"));

        appMembershipService.join("shop", existing.getId(), AppUser.AUTHORITY_APPLICATION_USER);

        assertTrue(appJoinRequestRepository.findAll().isEmpty());
    }

    @Test
    void 다른_애플리케이션의_신청은_승인할_수_없다() throws Exception {
        appMembershipService.saveSetting("crm", AppJoinPolicy.APPROVAL.name(), null);
        appMembershipService.applyForApplication("crm", existing.getId());
        Long requestId = appJoinRequestRepository.findAll().get(0).getId();

        mockMvc.perform(post("/shop/console/api/app-join/request/" + requestId + "/approve").cookie(tokenOf(owner)))
                .andExpect(status().isBadRequest());
        assertTrue(appUserRepository.findByApplicationIdAndUserId("crm", existing.getId()).isEmpty());
    }

    // ---- 가입 방식이 바뀐 경우 ----

    @Test
    void 인증_중에_초대만_받도록_바뀌면_계정만_만든다() throws Exception {
        policy(AppJoinPolicy.OPEN);
        String[] requested = requestAndCaptureCode();
        policy(AppJoinPolicy.INVITE_ONLY);

        verifyCode(requested)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"))
                .andExpect(jsonPath("$.redirectUrl").value("/"));

        User joiner = userRepository.findByLoginId("joiner").orElseThrow();
        assertTrue(appUserRepository.findByApplicationIdAndUserId("shop", joiner.getId()).isEmpty());
    }

    @Test
    void 앱_가입_요청은_공개_가입_API로_인증할_수_없다() throws Exception {
        policy(AppJoinPolicy.OPEN);
        String[] requested = requestAndCaptureCode();

        mockMvc.perform(post("/api/signup/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("verificationKey", requested[0], "code", requested[1]))))
                .andExpect(status().isBadRequest());
        assertTrue(userRepository.findByLoginId("joiner").isEmpty());
    }

    // ---- 콘솔: 가입 설정 ----

    @Test
    void 가입_설정을_저장하고_읽는다() throws Exception {
        mockMvc.perform(get("/shop/console/api/app-join/setting").cookie(tokenOf(owner)))
                .andExpect(jsonPath("$.joinPolicy").value("INVITE_ONLY"))
                .andExpect(jsonPath("$.defaultUserGroupId").doesNotExist());

        mockMvc.perform(put("/shop/console/api/app-join/setting").cookie(tokenOf(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinPolicy\":\"APPROVAL\",\"defaultUserGroupId\":" + defaultGroup.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinPolicy").value("APPROVAL"))
                .andExpect(jsonPath("$.defaultUserGroupId").value(defaultGroup.getId()));

        assertEquals(AppJoinPolicy.APPROVAL, appMembershipService.policyOf("shop"));
    }

    @Test
    void 잘못된_가입_방식이나_다른_애플리케이션의_그룹은_저장하지_않는다() throws Exception {
        UserGroup otherGroup = userGroupRepository.save(UserGroup.builder().applicationId("crm").companyCode("C001").code("CRM-G").name("고객팀").build());

        mockMvc.perform(put("/shop/console/api/app-join/setting").cookie(tokenOf(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinPolicy\":\"ANYONE\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/shop/console/api/app-join/setting").cookie(tokenOf(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinPolicy\":\"OPEN\",\"defaultUserGroupId\":" + otherGroup.getId() + "}"))
                .andExpect(status().isBadRequest());

        assertEquals(AppJoinPolicy.INVITE_ONLY, appMembershipService.policyOf("shop"));
    }

    @Test
    void 기본_그룹이_지워졌으면_그룹에_넣지_않고_가입만_한다() throws Exception {
        policy(AppJoinPolicy.OPEN);
        userGroupRepository.delete(defaultGroup);

        apply(existing).andExpect(jsonPath("$.status").value("MEMBER"));

        assertTrue(appUserRepository.findByApplicationIdAndUserId("shop", existing.getId()).isPresent());
        mockMvc.perform(get("/shop/console/api/app-join/setting").cookie(tokenOf(owner)))
                .andExpect(jsonPath("$.defaultUserGroupId").doesNotExist());
    }

    @Test
    void 관리자가_아니면_가입_설정과_신청을_볼_수_없다() throws Exception {
        mockMvc.perform(get("/shop/console/api/app-join/setting").cookie(tokenOf(existing)))
                .andExpect(redirectedUrl("/error/error403"));
        mockMvc.perform(get("/shop/console/api/app-join/request").cookie(tokenOf(existing)))
                .andExpect(redirectedUrl("/error/error403"));
    }
}
