package com.prometis.appbuilder.console.platform.rest;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 플랫폼 콘솔의 애플리케이션 관리자 지정 API.
 * 콘솔 인증 필터는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:application-admin;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class ApplicationAdminRestControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AppUserRepository appUserRepository;

    private Application ehr;
    private Application erp;
    private User kim;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.findByLoginId("kim").ifPresent(userRepository::delete);

        ehr = applicationRepository.save(Application.builder().applicationId("ehr").name("인사").status("ACTIVE").build());
        erp = applicationRepository.save(Application.builder().applicationId("erp").name("회계").status("ACTIVE").build());
        kim = userRepository.save(User.builder()
                .loginId("kim")
                .userName("김사용")
                .password("x")
                .email("kim@test.com")
                .build());
    }

    private ResultActions grant(Long applicationDbId, Long userId) throws Exception {
        return mockMvc.perform(post("/console/api/application/" + applicationDbId + "/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userId == null ? "{}" : "{\"userId\":" + userId + "}"));
    }

    @Test
    void 애플리케이션_사용자가_아니면_관리자로_새로_만든다() throws Exception {
        grant(ehr.getId(), kim.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("ehr"))
                .andExpect(jsonPath("$.loginId").value("kim"))
                .andExpect(jsonPath("$.authority").value(AppUser.AUTHORITY_APPLICATION_ADMIN));

        AppUser saved = appUserRepository.findByApplicationIdAndUserId("ehr", kim.getId()).orElseThrow();
        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN, saved.getAuthority());
    }

    @Test
    void 이미_애플리케이션_사용자면_권한만_관리자로_바꾼다() throws Exception {
        AppUser existing = appUserRepository.save(AppUser.builder()
                .applicationId("ehr")
                .userId(kim.getId())
                .authority("USER")
                .build());

        grant(ehr.getId(), kim.getId()).andExpect(status().isOk());

        List<AppUser> rows = appUserRepository.findAll();
        assertEquals(1, rows.size());
        assertEquals(existing.getId(), rows.get(0).getId());
        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN, rows.get(0).getAuthority());
    }

    @Test
    void 이미_관리자여도_다시_지정하면_그대로_한_행이다() throws Exception {
        grant(ehr.getId(), kim.getId()).andExpect(status().isOk());
        grant(ehr.getId(), kim.getId()).andExpect(status().isOk());

        assertEquals(1, appUserRepository.findAll().size());
    }

    @Test
    void 관리자_목록은_그_애플리케이션의_관리자만_사용자정보와_함께_준다() throws Exception {
        grant(ehr.getId(), kim.getId()).andExpect(status().isOk());
        User lee = userRepository.findByLoginId("lee").orElseGet(() -> userRepository.save(User.builder()
                .loginId("lee").userName("이사용").password("x").email("lee@test.com").build()));
        // 관리자가 아닌 사용자와 다른 애플리케이션의 관리자는 빠진다
        appUserRepository.save(AppUser.builder().applicationId("ehr").userId(lee.getId()).authority("USER").build());
        grant(erp.getId(), lee.getId()).andExpect(status().isOk());

        mockMvc.perform(get("/console/api/application/" + ehr.getId() + "/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(kim.getId()))
                .andExpect(jsonPath("$[0].userName").value("김사용"))
                .andExpect(jsonPath("$[0].email").value("kim@test.com"));
    }

    @Test
    void 사용자를_고르지_않았거나_없는_사용자면_400() throws Exception {
        grant(ehr.getId(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사용자를 선택해주세요."));

        grant(ehr.getId(), -1L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));

        assertEquals(0, appUserRepository.findAll().size());
    }

    @Test
    void 없는_애플리케이션이면_404() throws Exception {
        grant(-1L, kim.getId()).andExpect(status().isNotFound());
        mockMvc.perform(get("/console/api/application/-1/admin")).andExpect(status().isNotFound());
    }
}
