package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 애플리케이션 콘솔의 애플리케이션 사용자(AppUser) 등록 API.
 * 콘솔 인증 필터는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:app-user;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class AppUserRestControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AppUserRepository appUserRepository;

    private User kim;
    private User lee;
    private User park;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();

        kim = user("kim", "김사용");
        lee = user("lee", "이사용");
        park = user("park", "박사용");
    }

    private User user(String loginId, String userName) {
        return userRepository.findByLoginId(loginId).orElseGet(() -> userRepository.save(User.builder()
                .loginId(loginId)
                .userName(userName)
                .password("x")
                .email(loginId + "@test.com")
                .build()));
    }

    private ResultActions register(String body) throws Exception {
        return mockMvc.perform(post("/ehr/console/api/app-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 여러_명을_한번에_애플리케이션_사용자로_등록한다() throws Exception {
        register("{\"userIds\":[" + kim.getId() + "," + lee.getId() + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(2))
                .andExpect(jsonPath("$.skipped").value(0));

        assertEquals(AppUser.AUTHORITY_APPLICATION_USER,
                appUserRepository.findByApplicationIdAndUserId("ehr", kim.getId()).orElseThrow().getAuthority());
        assertEquals(2, appUserRepository.findByApplicationId("ehr").size());
    }

    @Test
    void 이미_등록된_사용자는_건너뛰고_관리자_권한도_바꾸지_않는다() throws Exception {
        appUserRepository.save(AppUser.builder()
                .applicationId("ehr")
                .userId(kim.getId())
                .authority(AppUser.AUTHORITY_APPLICATION_ADMIN)
                .build());

        register("{\"userIds\":[" + kim.getId() + "," + lee.getId() + "," + lee.getId() + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(1))
                .andExpect(jsonPath("$.skipped").value(1));

        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN,
                appUserRepository.findByApplicationIdAndUserId("ehr", kim.getId()).orElseThrow().getAuthority());
        assertEquals(2, appUserRepository.findByApplicationId("ehr").size());
    }

    @Test
    void User에_없는_대상이_섞여_있으면_아무도_등록하지_않는다() throws Exception {
        register("{\"userIds\":[" + kim.getId() + ",-1]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자가 포함되어 있습니다."));

        assertEquals(0, appUserRepository.findAll().size());
    }

    @Test
    void 선택한_사용자가_없으면_400() throws Exception {
        register("{\"userIds\":[]}").andExpect(status().isBadRequest());
        register("{}").andExpect(status().isBadRequest());
    }

    @Test
    void 등록_후보는_이_애플리케이션에_아직_없는_사용자이고_비밀번호는_내보내지_않는다() throws Exception {
        register("{\"userIds\":[" + kim.getId() + "]}").andExpect(status().isOk());
        // 다른 애플리케이션에 등록된 것은 상관없다
        appUserRepository.save(AppUser.builder().applicationId("erp").userId(lee.getId()).authority(AppUser.AUTHORITY_APPLICATION_USER).build());

        mockMvc.perform(get("/ehr/console/api/app-user/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", not(hasItem(kim.getId().intValue()))))
                .andExpect(jsonPath("$[*].userId", hasItems(lee.getId().intValue(), park.getId().intValue())))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void 등록된_사용자_목록은_그_애플리케이션_것만_사용자정보와_함께_준다() throws Exception {
        register("{\"userIds\":[" + kim.getId() + "]}").andExpect(status().isOk());
        appUserRepository.save(AppUser.builder().applicationId("erp").userId(lee.getId()).authority(AppUser.AUTHORITY_APPLICATION_USER).build());

        mockMvc.perform(get("/ehr/console/api/app-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(kim.getId()))
                .andExpect(jsonPath("$[0].userName").value("김사용"))
                .andExpect(jsonPath("$[0].authority").value(AppUser.AUTHORITY_APPLICATION_USER));
    }
}
