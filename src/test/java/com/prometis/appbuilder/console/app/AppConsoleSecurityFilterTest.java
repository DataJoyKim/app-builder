package com.prometis.appbuilder.console.app;

import com.prometis.appbuilder.app.security.AppAuthenticationService;
import com.prometis.appbuilder.app.security.NotAppAdminException;
import com.prometis.appbuilder.platform.application.ApplicationGuard;
import com.prometis.appbuilder.platform.application.ApplicationNotFoundException;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.exception.SecurityErrorMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppConsoleSecurityFilterTest {
    private AppAuthenticationService appAuthenticationService;
    private AppConsoleAccessValidator appConsoleAccessValidator;
    private ApplicationGuard applicationGuard;
    private AppConsoleSecurityFilter filter;

    @BeforeEach
    void setUp() {
        appAuthenticationService = mock(AppAuthenticationService.class);
        appConsoleAccessValidator = mock(AppConsoleAccessValidator.class);
        applicationGuard = mock(ApplicationGuard.class);
        filter = new AppConsoleSecurityFilter(appAuthenticationService, appConsoleAccessValidator, applicationGuard);
    }

    private AuthenticatedUser user() {
        return AuthenticatedUser.builder()
                .userId(7L)
                .userName("tester")
                .grantedPermissions(List.of())
                .build();
    }

    private record Result(MockHttpServletResponse response, MockFilterChain chain) {
        boolean passed() {
            return chain.getRequest() != null;
        }
    }

    private Result run(String uri) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", uri), response, chain);
        return new Result(response, chain);
    }

    @Test
    public void 콘솔_경로에서_applicationId를_꺼내_콘솔_접근을_검증한다() throws Exception {
        when(appAuthenticationService.authentication(any(), any())).thenReturn(user());

        Result result = run("/ehr/console/workflow-builder");

        assertTrue(result.passed());
        verify(appAuthenticationService).authentication(eq("ehr"), any());
        verify(appConsoleAccessValidator).validate("ehr", 7L);
    }

    @Test
    public void 콘솔_루트도_같은_규칙을_따른다() throws Exception {
        when(appAuthenticationService.authentication(any(), any())).thenReturn(user());

        assertTrue(run("/hr-portal/console").passed());
        verify(appConsoleAccessValidator).validate("hr-portal", 7L);
    }

    @Test
    public void 로그인하지_않으면_그_애플리케이션_콘솔로_돌아오는_401로_보낸다() throws Exception {
        when(appAuthenticationService.authentication(any(), any())).thenThrow(new SecurityBusinessException(SecurityErrorMessage.NOT_LOGIN));

        Result result = run("/ehr/console/workflow");

        assertFalse(result.passed());
        assertEquals("/error/error401?returnUrl=%2Fehr%2Fconsole", result.response().getRedirectedUrl());
        verifyNoInteractions(appConsoleAccessValidator);
    }

    @Test
    public void 콘솔_접근_권한이_없으면_403() throws Exception {
        when(appAuthenticationService.authentication(any(), any())).thenReturn(user());
        doThrow(new NotAppAdminException()).when(appConsoleAccessValidator).validate("ehr", 7L);

        Result result = run("/ehr/console/workflow");

        assertFalse(result.passed());
        assertEquals("/error/error403", result.response().getRedirectedUrl());
    }

    @Test
    public void 없는_애플리케이션의_콘솔이면_인증하지_않고_404로_보낸다() throws Exception {
        doThrow(new ApplicationNotFoundException()).when(applicationGuard).check("nope");

        Result result = run("/nope/console/workflow");

        assertFalse(result.passed());
        assertEquals("/error/error404", result.response().getRedirectedUrl());
        verifyNoInteractions(appAuthenticationService, appConsoleAccessValidator);
    }

    // /console/** 는 플랫폼 콘솔이라 PlatformConsoleSecurityFilter 가 맡는다. 이 필터는 건드리지 않는다.
    @ParameterizedTest
    @ValueSource(strings = {"/console", "/console/", "/console/workflow", "/console/api/workflow", "/console/console"})
    public void 플랫폼_콘솔_경로는_건드리지_않는다(String uri) throws Exception {
        Result result = run(uri);

        assertTrue(result.passed(), uri);
        verifyNoInteractions(applicationGuard, appAuthenticationService, appConsoleAccessValidator);
    }

    // 필터가 /* 에 걸려 있으므로 콘솔이 아닌 요청은 건드리지 않아야 한다.
    @ParameterizedTest
    @ValueSource(strings = {"/", "/ehr/workflow", "/ehr/rest/goals", "/console-plat/user", "/plugins/jquery/jquery.min.js",
            "/login", "/error/error401", "/ehr/consoles", "/ehr", "//console"})
    public void 콘솔이_아닌_경로는_그대로_통과한다(String uri) throws Exception {
        Result result = run(uri);

        assertTrue(result.passed(), uri);
        verifyNoInteractions(applicationGuard, appAuthenticationService, appConsoleAccessValidator);
    }

    // 관리자 가입 화면/API 는 계정이 없는 사람이 쓰므로 검사하지 않는다
    @ParameterizedTest
    @ValueSource(strings = {"/ehr/console/join", "/ehr/console/api/join/request", "/ehr/console/api/join/verify"})
    public void 가입_화면과_가입_API는_로그인_없이_통과한다(String uri) throws Exception {
        Result result = run(uri);

        assertTrue(result.passed(), uri);
        verifyNoInteractions(applicationGuard, appAuthenticationService, appConsoleAccessValidator);
    }

    // join 과 이름만 비슷한 콘솔 경로는 여전히 검사한다
    @ParameterizedTest
    @ValueSource(strings = {"/ehr/console/joined", "/ehr/console/api/join", "/ehr/console/api/joins/request", "/ehr/console/join/x"})
    public void 가입과_비슷한_다른_경로는_검사한다(String uri) throws Exception {
        when(appAuthenticationService.authentication(any(), any())).thenThrow(new SecurityBusinessException(SecurityErrorMessage.NOT_LOGIN));

        assertFalse(run(uri).passed(), uri);
    }

    @Test
    public void 컨텍스트_경로는_떼고_판단한다() throws Exception {
        when(appAuthenticationService.authentication(any(), any())).thenReturn(user());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/ehr/console/workflow");
        request.setContextPath("/app");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
        verify(appConsoleAccessValidator).validate("ehr", 7L);
    }

    @Test
    public void applicationId_추출() {
        assertEquals("ehr", AppConsoleSecurityFilter.applicationIdOf("/ehr/console"));
        assertEquals("ehr", AppConsoleSecurityFilter.applicationIdOf("/ehr/console/"));
        assertEquals("ehr", AppConsoleSecurityFilter.applicationIdOf("/ehr/console/api/workflow/1"));

        for(String path : List.of("/", "/ehr", "/ehr/", "/ehr/consoles", "/ehr/api/console", "//console", "", "/console/console")) {
            assertNull(AppConsoleSecurityFilter.applicationIdOf(path), path);
        }
    }
}
