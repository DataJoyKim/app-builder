package com.prometis.appbuilder.console;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 플랫폼 콘솔(console/platform)과 애플리케이션 콘솔(console/app) 화면이 실제로 렌더링되는지 확인한다.
 * 레이아웃 조각(~{/console/.../layout/...}) 경로가 틀리면 여기서 TemplateInputException 이 난다.
 * 콘솔 인증 필터는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:console-template-render;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class ConsoleTemplateRenderTest {
    @Autowired
    MockMvc mockMvc;

    // contents 디렉터리의 화면 이름 (content.html 은 화면을 감싸는 틀이라 뺀다)
    private static Stream<String> contentsOf(String console) throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/templates/console/" + console + "/contents/*.html");

        return Arrays.stream(resources)
                .map(Resource::getFilename)
                .filter(Objects::nonNull)
                .map(name -> name.substring(0, name.length() - ".html".length()))
                .filter(name -> !name.equals("content"));
    }

    static Stream<String> platformContents() throws IOException {
        return contentsOf("platform");
    }

    static Stream<String> appContents() throws IOException {
        return contentsOf("app");
    }

    @Test
    void 플랫폼_콘솔_첫화면이_렌더링된다() throws Exception {
        mockMvc.perform(get("/console")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("platformContents")
    void 플랫폼_콘솔_화면이_렌더링된다(String path) throws Exception {
        mockMvc.perform(get("/console/" + path)).andExpect(status().isOk());
    }

    @Test
    void 애플리케이션_콘솔_첫화면이_렌더링된다() throws Exception {
        mockMvc.perform(get("/ehr/console")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("appContents")
    void 애플리케이션_콘솔_화면이_렌더링된다(String path) throws Exception {
        mockMvc.perform(get("/ehr/console/" + path)).andExpect(status().isOk());
    }
}
