package com.prometis.appbuilder.platform.join;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import com.prometis.core.crypto.PasswordEncoder;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 공개 가입(/signup): 인증코드 메일 → 인증 → 계정 생성 + 로그인. 애플리케이션 권한은 생기지 않는다.
 * 메일은 JoinMailSender 를 목으로 바꿔 보낸 인증코드를 가로챈다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:signup;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false",
        "platform.join.verification-code-ttl=5m",
        "platform.join.max-verify-attempts=3",
        "platform.join.resend-cooldown=60s"
})
@AutoConfigureMockMvc
class SignupControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    JoinVerificationRepository joinVerificationRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @MockBean
    JoinMailSender joinMailSender;

    @BeforeEach
    void setUp() {
        joinVerificationRepository.deleteAll();
        appUserRepository.deleteAll();
        userRepository.findByLoginId("newbie").ifPresent(userRepository::delete);
    }

    private Map<String, String> signupInfo(String email) {
        Map<String, String> info = new HashMap<>();
        info.put("loginId", "newbie");
        info.put("userName", "신규");
        info.put("email", email);
        info.put("password", "pw1234!");
        info.put("checkPassword", "pw1234!");
        return info;
    }

    private ResultActions requestCode(Map<String, String> body) throws Exception {
        return mockMvc.perform(post("/api/signup/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions verifyCode(String verificationKey, String code) throws Exception {
        return mockMvc.perform(post("/api/signup/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("verificationKey", verificationKey, "code", code))));
    }

    // 인증코드 요청 → {verificationKey, 메일로 보낸 코드}
    private String[] requestAndCaptureCode(String email) throws Exception {
        String body = requestCode(signupInfo(email))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String verificationKey = objectMapper.readTree(body).get("verificationKey").asText();

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(joinMailSender, atLeastOnce()).sendVerificationCode(eq(email), anyString(), code.capture(), eq(Duration.ofMinutes(5)));

        return new String[]{verificationKey, code.getValue()};
    }

    @Test
    void 가입_화면은_로그인_없이_열린다() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/app/join"))
                .andExpect(model().attribute("mode", "SIGNUP"))
                .andExpect(model().attribute("codeTtlText", "5분"));
    }

    @Test
    void 인증코드를_맞히면_계정만_생기고_로그인되며_애플리케이션_만들기_화면으로_간다() throws Exception {
        String[] requested = requestAndCaptureCode("newbie@test.com");

        MvcResult result = verifyCode(requested[0], requested[1])
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").value("/"))
                .andExpect(cookie().exists("accessToken"))
                .andReturn();

        User user = userRepository.findByLoginId("newbie").orElseThrow();
        assertEquals("newbie@test.com", user.getEmail());
        assertTrue(passwordEncoder.matches("pw1234!", user.getPassword()));
        assertNull(user.getAuthority());
        assertTrue(appUserRepository.findByUserId(user.getId()).isEmpty());
        assertTrue(joinVerificationRepository.findAll().isEmpty());

        Cookie accessToken = result.getResponse().getCookie("accessToken");
        mockMvc.perform(get("/").cookie(accessToken))
                .andExpect(redirectedUrl("/applications/manage"));
    }

    @Test
    void 인증코드와_비밀번호는_해시로만_저장하고_인증_전에는_계정이_없다() throws Exception {
        String[] requested = requestAndCaptureCode("newbie@test.com");

        JoinVerification saved = joinVerificationRepository.findByVerificationKey(requested[0]).orElseThrow();
        assertNotEquals(requested[1], saved.getEncodedCode());
        assertNotEquals("pw1234!", saved.getEncodedPassword());
        assertNull(saved.getApplicationId());
        assertNull(saved.getInvitationId());
        assertTrue(userRepository.findByLoginId("newbie").isEmpty());
    }

    @Test
    void 인증코드를_틀리면_남은_횟수를_알려주고_횟수를_넘기면_요청을_버린다() throws Exception {
        String[] requested = requestAndCaptureCode("newbie@test.com");
        String wrong = requested[1].equals("000000") ? "111111" : "000000";

        verifyCode(requested[0], wrong)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("남은 입력 횟수 2회")));
        verifyCode(requested[0], wrong)
                .andExpect(jsonPath("$.message", containsString("남은 입력 횟수 1회")));
        verifyCode(requested[0], wrong)
                .andExpect(jsonPath("$.message", containsString("3회 잘못 입력")));

        verifyCode(requested[0], requested[1])
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("가입 요청을 찾을 수 없습니다")));
        assertTrue(userRepository.findByLoginId("newbie").isEmpty());
    }

    @Test
    void 만료된_인증코드로는_가입할_수_없다() throws Exception {
        joinVerificationRepository.save(JoinVerification.builder()
                .verificationKey("expired-key")
                .loginId("newbie")
                .userName("신규")
                .email("newbie@test.com")
                .encodedPassword(passwordEncoder.encode("pw1234!"))
                .encodedCode(passwordEncoder.encode("123456"))
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .failCount(0)
                .createdAt(LocalDateTime.now().minusMinutes(6))
                .build());

        verifyCode("expired-key", "123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("만료")));

        assertTrue(userRepository.findByLoginId("newbie").isEmpty());
        assertTrue(joinVerificationRepository.findByVerificationKey("expired-key").isEmpty());
    }

    @Test
    void 같은_이메일로는_잠시_후에_다시_받을_수_있다() throws Exception {
        requestAndCaptureCode("newbie@test.com");

        requestCode(signupInfo("newbie@test.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("초 후에 다시 받을 수 있습니다")));
    }

    @Test
    void 같은_로그인ID로_다시_요청하면_이전_인증코드는_쓸_수_없다() throws Exception {
        String[] first = requestAndCaptureCode("first@test.com");
        String[] second = requestAndCaptureCode("second@test.com");

        verifyCode(first[0], first[1])
                .andExpect(status().isBadRequest());
        verifyCode(second[0], second[1])
                .andExpect(status().isOk());
        assertEquals("second@test.com", userRepository.findByLoginId("newbie").orElseThrow().getEmail());
    }

    @Test
    void 공개_가입_요청은_초대_합류_API로_인증할_수_없다() throws Exception {
        String[] requested = requestAndCaptureCode("newbie@test.com");

        mockMvc.perform(post("/ehr/console/api/join/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("verificationKey", requested[0], "code", requested[1]))))
                .andExpect(status().isBadRequest());
        assertTrue(userRepository.findByLoginId("newbie").isEmpty());
    }

    @Test
    void 이미_있는_로그인ID나_잘못된_입력이면_메일을_보내지_않는다() throws Exception {
        if(userRepository.findByLoginId("taken").isEmpty()) {
            userRepository.save(User.builder().loginId("taken").userName("기존").password("x").email("taken@test.com").build());
        }

        Map<String, String> taken = signupInfo("newbie@test.com");
        taken.put("loginId", "taken");
        requestCode(taken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 사용 중인 로그인ID입니다."));

        requestCode(signupInfo("not-an-email")).andExpect(status().isBadRequest());

        Map<String, String> mismatch = signupInfo("newbie@test.com");
        mismatch.put("checkPassword", "other");
        requestCode(mismatch).andExpect(status().isBadRequest());

        verifyNoInteractions(joinMailSender);
        assertTrue(joinVerificationRepository.findAll().isEmpty());
    }

    @Test
    void 메일을_보내지_못하면_가입_요청을_남기지_않는다() throws Exception {
        doThrow(new JoinException("인증 메일을 보내지 못했습니다."))
                .when(joinMailSender).sendVerificationCode(anyString(), anyString(), anyString(), any());

        requestCode(signupInfo("newbie@test.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 메일을 보내지 못했습니다."));

        assertTrue(joinVerificationRepository.findAll().isEmpty());
    }

    @Test
    void 기간_표시() {
        assertEquals("5분", JoinMailSender.describe(Duration.ofMinutes(5)));
        assertEquals("7일", JoinMailSender.describe(Duration.ofDays(7)));
        assertEquals("2시간", JoinMailSender.describe(Duration.ofHours(2)));
        assertEquals("90초", JoinMailSender.describe(Duration.ofSeconds(90)));
    }
}
