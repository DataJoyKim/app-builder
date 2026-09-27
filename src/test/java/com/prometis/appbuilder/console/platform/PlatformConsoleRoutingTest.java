package com.prometis.appbuilder.console.platform;

import com.prometis.appbuilder.console.app.view.ConsoleViewController;
import com.prometis.appbuilder.console.platform.rest.ApplicationRestController;
import com.prometis.appbuilder.console.platform.rest.UserRestController;
import com.prometis.appbuilder.console.platform.view.PlatformConsoleViewController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 플랫폼 콘솔(/console/**)과 애플리케이션 콘솔(/{applicationId}/console/**)이 서로의 요청을 가로채지 않는지 확인한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:platform-console-routing;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
class PlatformConsoleRoutingTest {
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    private Class<?> handlerOf(String method, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        HandlerExecutionChain chain = handlerMapping.getHandler(request);
        assertNotNull(chain, uri);
        return ((HandlerMethod) chain.getHandler()).getBeanType();
    }

    @Test
    void 플랫폼_콘솔_경로는_플랫폼_컨트롤러로_간다() throws Exception {
        assertEquals(PlatformConsoleViewController.class, handlerOf("GET", "/console"));
        assertEquals(PlatformConsoleViewController.class, handlerOf("GET", "/console/application"));
        assertEquals(PlatformConsoleViewController.class, handlerOf("GET", "/console/user"));
        assertEquals(ApplicationRestController.class, handlerOf("GET", "/console/api/application"));
        assertEquals(ApplicationRestController.class, handlerOf("GET", "/console/api/application/reserved-ids"));
        assertEquals(UserRestController.class, handlerOf("GET", "/console/api/user"));
    }

    @Test
    void 애플리케이션_콘솔_경로는_애플리케이션_컨트롤러로_간다() throws Exception {
        assertEquals(ConsoleViewController.class, handlerOf("GET", "/ehr/console"));
        assertEquals(ConsoleViewController.class, handlerOf("GET", "/ehr/console/workflow"));
    }

    @Test
    void 플랫폼_콘솔_화면은_console_platform_템플릿을_쓴다() {
        PlatformConsoleViewController controller = new PlatformConsoleViewController();
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("console/platform/index", controller.moveIndex());
        assertEquals("console/platform/contents/content", controller.moveDataSource(model, "application"));
        assertEquals("/console/platform/contents/application", model.get("contentPath"));
    }

    @Test
    void 애플리케이션_콘솔_화면은_console_app_템플릿을_쓴다() {
        ConsoleViewController controller = new ConsoleViewController();
        ExtendedModelMap indexModel = new ExtendedModelMap();
        ExtendedModelMap contentModel = new ExtendedModelMap();

        assertEquals("console/app/index", controller.moveIndex(indexModel, "ehr"));
        assertEquals("ehr", indexModel.get("applicationId"));

        assertEquals("console/app/contents/content", controller.moveDataSource(contentModel, "ehr", "workflow"));
        assertEquals("ehr", contentModel.get("applicationId"));
        assertEquals("/console/app/contents/workflow", contentModel.get("contentPath"));
    }

    @Test
    void console_console_도_모호하지_않다() throws Exception {
        // /console/{path} 와 /{applicationId}/console 이 둘 다 맞는 경로라 매핑이 모호하면 여기서 예외가 난다.
        assertEquals(PlatformConsoleViewController.class, handlerOf("GET", "/console/console"));
    }
}
