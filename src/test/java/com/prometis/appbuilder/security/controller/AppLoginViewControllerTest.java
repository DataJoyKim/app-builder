package com.prometis.appbuilder.security.controller;

import com.prometis.appbuilder.app.view.LayoutRepository;
import com.prometis.appbuilder.app.view.domain.Layout;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.application.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 애플리케이션 로그인 화면(/{applicationId}/login)이 Layout 의 로그인 이미지 로고 링크를 로고로 보여주고,
 * 설정이 없으면 애플리케이션 이름을 보여주는지 확인한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:app-login;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc
class AppLoginViewControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    LayoutRepository layoutRepository;

    @BeforeEach
    void setUp() {
        layoutRepository.deleteAll();
        application("ehr", "인사관리");
        application("crm", "고객관리");
    }

    private void application(String applicationId, String name) {
        if(applicationRepository.findByApplicationId(applicationId).isEmpty()) {
            applicationRepository.save(Application.builder()
                    .applicationId(applicationId)
                    .name(name)
                    .status(ApplicationStatus.ACTIVE.name())
                    .build());
        }
    }

    @Test
    void 로그인_이미지_로고_링크가_있으면_이미지로_보여준다() throws Exception {
        layoutRepository.save(Layout.builder()
                .applicationId("ehr")
                .loginImgLogoLink("/ehr/image/logo/main")
                .build());

        mockMvc.perform(get("/ehr/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/app-login"))
                .andExpect(model().attribute("applicationName", "인사관리"))
                .andExpect(model().attribute("loginImgLogoLink", "/ehr/image/logo/main"))
                .andExpect(content().string(containsString("src=\"/ehr/image/logo/main\"")));
    }

    @Test
    void 링크_설정이_없으면_애플리케이션_이름을_보여준다() throws Exception {
        // Layout 행이 없는 애플리케이션
        mockMvc.perform(get("/crm/login"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("loginImgLogoLink", nullValue()))
                .andExpect(content().string(containsString(">고객관리</span>")))
                .andExpect(content().string(not(containsString("id=\"logoImg\""))));

        // 링크가 공백뿐이면 설정이 없는 것으로 본다
        layoutRepository.save(Layout.builder()
                .applicationId("crm")
                .loginImgLogoLink("  ")
                .build());

        mockMvc.perform(get("/crm/login"))
                .andExpect(model().attribute("loginImgLogoLink", nullValue()));
    }

    @Test
    void 로그인하지_않고_앱_화면에_오면_앱_로그인_화면으로_보낸다() throws Exception {
        // 앱 첫 화면(/{applicationId})은 Layout 설정과 상관없이 항상 로그인이 필요하다
        mockMvc.perform(get("/ehr"))
                .andExpect(view().name("/error/error401"))
                .andExpect(model().attribute("loginUrl", "/ehr/login"))
                // th:inline="javascript" 는 / 를 \/ 로 이스케이프한다
                .andExpect(content().string(containsString("const appLoginUrl = \"\\/ehr\\/login\"")));
    }

    @Test
    void 없는_애플리케이션이면_404_화면() throws Exception {
        mockMvc.perform(get("/nothing/login"))
                .andExpect(view().name("/error/error404"));
    }
}
