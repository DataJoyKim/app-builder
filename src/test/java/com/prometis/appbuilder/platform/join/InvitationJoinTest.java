package com.prometis.appbuilder.platform.join;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 애플리케이션 관리자 초대 합류.
 * 콘솔(관리자)에서 이메일로 초대 → 링크(/{applicationId}/console/join?token=...)에서 그 이메일로 가입하거나 기존 계정으로 수락 → 관리자.
 * 콘솔 보안 필터를 그대로 거친다. 메일은 JoinMailSender 를 목으로 바꾼다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:invitation-join;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false",
        "platform.join.verification-code-ttl=5m",
        "platform.join.resend-cooldown=0s",
        "platform.join.invitation-ttl=7d"
})
@AutoConfigureMockMvc
class InvitationJoinTest {
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
    AppInvitationRepository appInvitationRepository;
    @Autowired
    JoinVerificationRepository joinVerificationRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    UserGroupUserRepository userGroupUserRepository;
    @Autowired
    AppMembershipService appMembershipService;
    @Autowired
    AppJoinSettingRepository appJoinSettingRepository;
    @MockBean
    JoinMailSender joinMailSender;

    private User owner;

    @BeforeEach
    void setUp() {
        joinVerificationRepository.deleteAll();
        appInvitationRepository.deleteAll();
        appUserRepository.deleteAll();
        appJoinSettingRepository.deleteAll();
        userGroupUserRepository.deleteAll();
        userGroupRepository.deleteAll();
        userRepository.findByLoginId("invitee").ifPresent(userRepository::delete);

        application("ehr", "인사관리");
        application("crm", "고객관리");

        owner = user("inv-owner", "소유자", "owner@test.com");
        appUserRepository.save(AppUser.builder().applicationId("ehr").userId(owner.getId()).authority(AppUser.AUTHORITY_APPLICATION_ADMIN).build());
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

    // 콘솔에서 초대 → 응답 JSON
    private JsonNode invite(String applicationId, String email) throws Exception {
        return invite(applicationId, email, null);
    }

    // authority 가 null 이면 보내지 않는다 (관리자 초대)
    private JsonNode invite(String applicationId, String email, String authority) throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        if(authority != null) {
            request.put("authority", authority);
        }

        String body = mockMvc.perform(post("/" + applicationId + "/console/api/app-invitation").cookie(tokenOf(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String tokenOfLink(String link) {
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    private Map<String, String> joinInfo(String token, String email) {
        Map<String, String> info = new HashMap<>();
        info.put("loginId", "invitee");
        info.put("userName", "초대받은사람");
        info.put("email", email);
        info.put("password", "pw1234!");
        info.put("checkPassword", "pw1234!");
        info.put("token", token);
        return info;
    }

    private ResultActions requestCode(String applicationId, Map<String, String> body) throws Exception {
        return mockMvc.perform(post("/" + applicationId + "/console/api/join/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions verifyCode(String applicationId, String verificationKey, String code) throws Exception {
        return mockMvc.perform(post("/" + applicationId + "/console/api/join/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("verificationKey", verificationKey, "code", code))));
    }

    private ResultActions accept(String applicationId, String token, User loginUser) throws Exception {
        var request = post("/" + applicationId + "/console/api/join/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token)));
        if(loginUser != null) {
            request.cookie(tokenOf(loginUser));
        }
        return mockMvc.perform(request);
    }

    // 초대 가입 인증코드 요청 → {verificationKey, 코드}
    private String[] requestAndCaptureCode(String token, String bodyEmail, String expectedEmail) throws Exception {
        String body = requestCode("ehr", joinInfo(token, bodyEmail))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(joinMailSender, atLeastOnce()).sendVerificationCode(eq(expectedEmail), eq("인사관리 관리자 합류"), code.capture(), any());

        return new String[]{objectMapper.readTree(body).get("verificationKey").asText(), code.getValue()};
    }

    // ---- 콘솔: 초대 만들기/목록/취소 ----

    @Test
    void 관리자가_초대하면_링크를_만들고_초대_메일을_보낸다() throws Exception {
        when(joinMailSender.sendInvitation(anyString(), anyString(), anyString(), any(), anyString(), any())).thenReturn(true);

        JsonNode invitation = invite("ehr", "invitee@test.com");

        assertEquals("PENDING", invitation.get("status").asText());
        assertTrue(invitation.get("mailSent").asBoolean());
        String link = invitation.get("link").asText();
        assertTrue(link.startsWith("http://localhost/ehr/console/join?token="), link);
        verify(joinMailSender).sendInvitation(eq("invitee@test.com"), eq("인사관리"), eq("관리자"), eq("소유자"), eq(link), eq(Duration.ofDays(7)));

        mockMvc.perform(get("/ehr/console/api/app-invitation").cookie(tokenOf(owner)))
                .andExpect(jsonPath("$[0].email").value("invitee@test.com"))
                .andExpect(jsonPath("$[0].link").value(link));
    }

    @Test
    void 메일을_보내지_못해도_초대는_남고_링크를_돌려준다() throws Exception {
        JsonNode invitation = invite("ehr", "invitee@test.com");

        assertFalse(invitation.get("mailSent").asBoolean());
        assertNotNull(invitation.get("link").asText());
        assertEquals(1, appInvitationRepository.findAll().size());
    }

    @Test
    void 관리자가_아니면_초대할_수_없다() throws Exception {
        User stranger = user("inv-stranger", "남", "stranger@test.com");

        mockMvc.perform(post("/ehr/console/api/app-invitation").cookie(tokenOf(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@test.com\"}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/error/error403"));
        assertTrue(appInvitationRepository.findAll().isEmpty());
    }

    // ---- 초대 화면 ----

    @Test
    void 초대_화면은_로그인_없이_열리고_초대받은_이메일을_보여준다() throws Exception {
        String token = tokenOfLink(invite("ehr", "invitee@test.com").get("link").asText());

        mockMvc.perform(get("/ehr/console/join").param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("console/app/join"))
                .andExpect(model().attribute("mode", "INVITE"))
                .andExpect(model().attribute("invitationEmail", "invitee@test.com"))
                .andExpect(model().attributeDoesNotExist("invitationError"));
    }

    @Test
    void 토큰이_없거나_틀리면_초대_화면에_이유를_보여준다() throws Exception {
        mockMvc.perform(get("/ehr/console/join"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("invitationError", containsString("초대 링크가 올바르지 않습니다")));
        mockMvc.perform(get("/ehr/console/join").param("token", "nope"))
                .andExpect(model().attribute("invitationError", containsString("유효하지 않은 초대 링크")));
    }

    @Test
    void 다른_애플리케이션의_초대_토큰은_쓸_수_없다() throws Exception {
        String token = tokenOfLink(invite("ehr", "invitee@test.com").get("link").asText());

        mockMvc.perform(get("/crm/console/join").param("token", token))
                .andExpect(model().attribute("invitationError", containsString("유효하지 않은 초대 링크")));
        requestCode("crm", joinInfo(token, "invitee@test.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 없는_애플리케이션의_초대_화면은_404() throws Exception {
        mockMvc.perform(get("/nope/console/join").param("token", "x"))
                .andExpect(view().name("/error/error404"));
    }

    // ---- 새 계정으로 합류 ----

    @Test
    void 초대받은_이메일로_가입하면_관리자가_되고_초대는_사용된다() throws Exception {
        String token = tokenOfLink(invite("ehr", "invitee@test.com").get("link").asText());

        // 화면이 다른 이메일을 보내도 초대받은 이메일로만 보낸다
        String[] requested = requestAndCaptureCode(token, "other@test.com", "invitee@test.com");

        verifyCode("ehr", requested[0], requested[1])
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").value("/"))
                .andExpect(cookie().exists("accessToken"));

        User invitee = userRepository.findByLoginId("invitee").orElseThrow();
        assertEquals("invitee@test.com", invitee.getEmail());
        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN,
                appUserRepository.findByApplicationIdAndUserId("ehr", invitee.getId()).orElseThrow().getAuthority());

        AppInvitation used = appInvitationRepository.findByToken(token).orElseThrow();
        assertTrue(used.isAccepted());
        assertEquals(invitee.getId(), used.getAcceptedUserId());

        // 한 번 쓴 링크는 다시 쓸 수 없다
        requestCode("ehr", joinInfo(token, "invitee@test.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("이미 사용된 초대 링크")));
    }

    @Test
    void 토큰_없이는_초대_합류_인증코드를_받을_수_없다() throws Exception {
        requestCode("ehr", joinInfo(null, "invitee@test.com"))
                .andExpect(status().isBadRequest());
        verify(joinMailSender, never()).sendVerificationCode(anyString(), anyString(), anyString(), any());
    }

    @Test
    void 만료된_초대로는_합류할_수_없다() throws Exception {
        appInvitationRepository.save(AppInvitation.builder()
                .token("expired-token")
                .applicationId("ehr")
                .email("invitee@test.com")
                .createdAt(LocalDateTime.now().minusDays(8))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build());

        requestCode("ehr", joinInfo("expired-token", "invitee@test.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("만료된 초대 링크")));
    }

    @Test
    void 인증_중에_초대를_취소하면_가입할_수_없다() throws Exception {
        JsonNode invitation = invite("ehr", "invitee@test.com");
        String token = tokenOfLink(invitation.get("link").asText());
        String[] requested = requestAndCaptureCode(token, "invitee@test.com", "invitee@test.com");

        mockMvc.perform(delete("/ehr/console/api/app-invitation/" + invitation.get("id").asLong()).cookie(tokenOf(owner)))
                .andExpect(status().isOk());

        verifyCode("ehr", requested[0], requested[1])
                .andExpect(status().isBadRequest());
        assertTrue(userRepository.findByLoginId("invitee").isEmpty());
    }

    @Test
    void 같은_이메일을_다시_초대하면_이전_링크는_쓸_수_없다() throws Exception {
        String first = tokenOfLink(invite("ehr", "invitee@test.com").get("link").asText());
        String second = tokenOfLink(invite("ehr", "INVITEE@test.com").get("link").asText());

        assertNotEquals(first, second);
        requestCode("ehr", joinInfo(first, "invitee@test.com"))
                .andExpect(status().isBadRequest());
        requestCode("ehr", joinInfo(second, "invitee@test.com"))
                .andExpect(status().isOk());
    }

    // ---- 기존 계정으로 수락 ----

    @Test
    void 초대받은_이메일의_기존_계정은_로그인해서_수락하면_관리자가_된다() throws Exception {
        User existing = user("inv-existing", "기존사용자", "Existing@test.com");
        // 이미 사용자로 등록되어 있었으면 관리자로 바뀐다
        appUserRepository.save(AppUser.builder().applicationId("ehr").userId(existing.getId()).authority(AppUser.AUTHORITY_APPLICATION_USER).build());
        String token = tokenOfLink(invite("ehr", "existing@test.com").get("link").asText());

        mockMvc.perform(get("/ehr/console/join").param("token", token).cookie(tokenOf(existing)))
                .andExpect(model().attribute("loginEmailMatches", true));

        accept("ehr", token, existing)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").value("/"));

        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN,
                appUserRepository.findByApplicationIdAndUserId("ehr", existing.getId()).orElseThrow().getAuthority());
        assertTrue(appInvitationRepository.findByToken(token).orElseThrow().isAccepted());

        accept("ehr", token, existing)
                .andExpect(status().isBadRequest());
    }

    @Test
    void 다른_이메일의_계정으로는_수락할_수_없다() throws Exception {
        User other = user("inv-other", "다른사람", "other@test.com");
        String token = tokenOfLink(invite("ehr", "invitee@test.com").get("link").asText());

        mockMvc.perform(get("/ehr/console/join").param("token", token).cookie(tokenOf(other)))
                .andExpect(model().attribute("loginEmailMatches", false));

        accept("ehr", token, other)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invitee@test.com")));
        assertTrue(appUserRepository.findByApplicationIdAndUserId("ehr", other.getId()).isEmpty());
        assertFalse(appInvitationRepository.findByToken(token).orElseThrow().isAccepted());
    }

    // ---- 사용자 초대 ----

    @Test
    void 사용자로_초대하면_사용자가_되고_기본_사용자_그룹에_들어간다() throws Exception {
        UserGroup group = userGroupRepository.save(UserGroup.builder().applicationId("ehr").code("INV-DEFAULT").name("기본").build());
        appMembershipService.saveSetting("ehr", "INVITE_ONLY", group.getId());

        JsonNode invitation = invite("ehr", "invitee@test.com", AppUser.AUTHORITY_APPLICATION_USER);
        assertEquals(AppUser.AUTHORITY_APPLICATION_USER, invitation.get("authority").asText());
        String token = tokenOfLink(invitation.get("link").asText());

        mockMvc.perform(get("/ehr/console/join").param("token", token))
                .andExpect(model().attribute("invitationRoleLabel", "사용자"));

        String body = requestCode("ehr", joinInfo(token, "invitee@test.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(joinMailSender).sendVerificationCode(eq("invitee@test.com"), eq("인사관리 사용자 합류"), code.capture(), any());

        verifyCode("ehr", objectMapper.readTree(body).get("verificationKey").asText(), code.getValue())
                .andExpect(status().isOk());

        User invitee = userRepository.findByLoginId("invitee").orElseThrow();
        assertEquals(AppUser.AUTHORITY_APPLICATION_USER,
                appUserRepository.findByApplicationIdAndUserId("ehr", invitee.getId()).orElseThrow().getAuthority());
        assertTrue(userGroupUserRepository.findByUserGroupId(group.getId()).stream()
                .anyMatch(member -> member.getUser().getId().equals(invitee.getId())));
    }

    @Test
    void 사용자_초대로는_관리자를_낮추지_않는다() throws Exception {
        User admin = user("inv-admin2", "다른관리자", "admin2@test.com");
        appUserRepository.save(AppUser.builder().applicationId("ehr").userId(admin.getId()).authority(AppUser.AUTHORITY_APPLICATION_ADMIN).build());
        String token = tokenOfLink(invite("ehr", "admin2@test.com", AppUser.AUTHORITY_APPLICATION_USER).get("link").asText());

        accept("ehr", token, admin)
                .andExpect(status().isOk());

        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN,
                appUserRepository.findByApplicationIdAndUserId("ehr", admin.getId()).orElseThrow().getAuthority());
    }

    // ---- 여러 명 초대 ----

    private ResultActions inviteAll(String body) throws Exception {
        return mockMvc.perform(post("/ehr/console/api/app-invitation/batch").cookie(tokenOf(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 여러_이메일을_한번에_초대하고_잘못된_이메일만_실패한다() throws Exception {
        inviteAll("{\"emails\":[\"a@test.com\",\"not-email\",\"b@test.com\",\"A@TEST.com\"],\"authority\":\"APPLICATION_USER\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invited").value(2))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.results[0].email").value("a@test.com"))
                .andExpect(jsonPath("$.results[0].success").value(true))
                .andExpect(jsonPath("$.results[0].link", containsString("/ehr/console/join?token=")))
                .andExpect(jsonPath("$.results[1].success").value(false))
                .andExpect(jsonPath("$.results[1].message", containsString("이메일 형식")));

        assertEquals(2, appInvitationRepository.findAll().size());
        assertTrue(appInvitationRepository.findAll().stream()
                .allMatch(invitation -> AppUser.AUTHORITY_APPLICATION_USER.equals(invitation.getAuthority())));
    }

    @Test
    void 여러_명_초대는_비어_있거나_너무_많으면_거절한다() throws Exception {
        inviteAll("{\"emails\":[],\"authority\":\"APPLICATION_USER\"}")
                .andExpect(status().isBadRequest());

        StringBuilder emails = new StringBuilder();
        for(int i = 0; i < 51; i++) {
            emails.append(i == 0 ? "" : ",").append("\"user").append(i).append("@test.com\"");
        }
        inviteAll("{\"emails\":[" + emails + "],\"authority\":\"APPLICATION_USER\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("50명")));

        assertTrue(appInvitationRepository.findAll().isEmpty());
    }

    @Test
    void 알_수_없는_권한으로는_초대할_수_없다() throws Exception {
        mockMvc.perform(post("/ehr/console/api/app-invitation").cookie(tokenOf(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@test.com\",\"authority\":\"PLATFORM_ADMIN\"}"))
                .andExpect(status().isBadRequest());
        assertTrue(appInvitationRepository.findAll().isEmpty());
    }

    @Test
    void 로그인하지_않고_수락하면_401() throws Exception {
        String token = tokenOfLink(invite("ehr", "invitee@test.com").get("link").asText());

        accept("ehr", token, null)
                .andExpect(status().isUnauthorized());
    }
}
