package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.code.ObjectType;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.application.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 파일(FILE) 유형 화면(/{applicationId}/pages/{objectCode})이 {root-path}/{applicationId}/views/{objectCode}.html 의 소스를
 * 템플릿으로 해석하지 않고 글자 그대로 넣는지 확인한다. (소스 안의 Thymeleaf 식이 서버에서 실행되면 안 된다)
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:view-file;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc
class ViewBuilderFileViewTest {
    private static final String APP = "ehr";

    @TempDir
    static Path viewRoot;

    @DynamicPropertySource
    static void viewRootPath(DynamicPropertyRegistry registry) {
        registry.add("platform.view.root-path", () -> viewRoot.toString());
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    ViewObjectRepository viewObjectRepository;
    @Autowired
    ViewSourceStorage viewSourceStorage;

    @BeforeEach
    void setUp() {
        if(applicationRepository.findByApplicationId(APP).isEmpty()) {
            applicationRepository.save(Application.builder()
                    .applicationId(APP)
                    .name("인사관리")
                    .status(ApplicationStatus.ACTIVE.name())
                    .build());
        }
        viewObjectRepository.deleteAll();
    }

    private void fileObject(String objectCode) {
        viewObjectRepository.save(ViewObject.builder()
                .applicationId(APP)
                .objectCode(objectCode)
                .objectName("화면")
                .type(ObjectType.FILE)
                .useAuthValidation(false)
                .usePermissionValidation(false)
                .build());
    }

    @Test
    void 소스를_템플릿으로_해석하지_않고_그대로_넣는다() throws Exception {
        fileObject("EMP_LIST");
        viewSourceStorage.write(APP, "EMP_LIST",
                "<div id=\"emp\">사원</div><span th:text=\"${7*7}\">x</span>[[${7*191}]]");

        mockMvc.perform(get("/" + APP + "/pages/EMP_LIST"))
                .andExpect(status().isOk())
                .andExpect(view().name("template/mf-template"))
                .andExpect(content().string(containsString("<div id=\"emp\">사원</div>")))
                .andExpect(content().string(containsString("th:text=\"${7*7}\"")))
                .andExpect(content().string(containsString("[[${7*191}]]")))
                .andExpect(content().string(not(containsString("1337"))));
    }

    @Test
    void 소스가_없으면_404_화면() throws Exception {
        fileObject("NO_SOURCE");

        mockMvc.perform(get("/" + APP + "/pages/NO_SOURCE"))
                .andExpect(view().name("error/error404"));
    }
}
