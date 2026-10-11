package com.prometis.appbuilder.console.platform.rest;

import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import com.prometis.core.crypto.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 플랫폼 콘솔 사용자 화면의 비밀번호 변경 API (PUT /console/api/user/{id}/password).
 * 콘솔 인증 필터는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-password;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class UserPasswordRestControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private User kim;

    @BeforeEach
    void setUp() {
        userRepository.findByLoginId("pw-kim").ifPresent(userRepository::delete);
        kim = userRepository.save(User.builder()
                .loginId("pw-kim")
                .userName("김사용")
                .password(passwordEncoder.encode("old-password"))
                .email("pw-kim@test.com")
                .build());
    }

    private ResultActions changePassword(Long id, String body) throws Exception {
        return mockMvc.perform(put("/console/api/user/" + id + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String savedPassword() {
        return userRepository.findById(kim.getId()).orElseThrow().getPassword();
    }

    @Test
    void 새_비밀번호를_해시로_저장한다() throws Exception {
        changePassword(kim.getId(), "{\"password\":\"new-password\",\"checkPassword\":\"new-password\"}")
                .andExpect(status().isOk());

        String saved = savedPassword();
        assertNotEquals("new-password", saved);
        assertTrue(passwordEncoder.matches("new-password", saved));
        assertFalse(passwordEncoder.matches("old-password", saved));
    }

    @Test
    void 비어_있거나_확인이_다르면_바꾸지_않는다() throws Exception {
        changePassword(kim.getId(), "{\"password\":\"\",\"checkPassword\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("새 비밀번호를 입력해주세요."));

        changePassword(kim.getId(), "{\"password\":\"new-password\",\"checkPassword\":\"other\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("새 비밀번호 확인이 일치하지 않습니다."));

        assertTrue(passwordEncoder.matches("old-password", savedPassword()));
    }

    @Test
    void 없는_사용자면_404() throws Exception {
        changePassword(-1L, "{\"password\":\"new-password\",\"checkPassword\":\"new-password\"}")
                .andExpect(status().isNotFound());
    }
}
