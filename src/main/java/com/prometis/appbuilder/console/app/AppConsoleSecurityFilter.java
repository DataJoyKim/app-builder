package com.prometis.appbuilder.console.app;

import com.prometis.appbuilder.app.security.AppAuthenticationService;
import com.prometis.appbuilder.app.security.NotAppAdminException;
import com.prometis.appbuilder.app.security.appuser.AppUserAccessValidator;
import com.prometis.appbuilder.platform.application.ApplicationGuard;
import com.prometis.appbuilder.platform.application.ApplicationNotFoundException;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.token.TokenCookie;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 애플리케이션 콘솔(/{applicationId}/console/**) 접근을 막는다.
 * 서블릿 URL 패턴은 중간 와일드카드(/*&#47;console/*)를 지원하지 않아 /* 에 등록하고, 콘솔 경로인지는 여기서 직접 가린다.
 * - /{applicationId}/console, /{applicationId}/console/... : 로그인 + 애플리케이션 관리자(또는 플랫폼관리자)인지 검사한다.
 * - /console, /console/... : 플랫폼 콘솔이라 PlatformConsoleSecurityFilter 가 맡으므로 여기서는 건드리지 않는다.
 *   (/console/console 도 applicationId 가 "console" 인 애플리케이션 콘솔로 보지 않는다. console 은 예약된 ID 다)
 * - 그 밖의 경로 : 콘솔이 아니므로 그대로 통과시킨다.
 */
@RequiredArgsConstructor
public class AppConsoleSecurityFilter implements Filter {
    private static final String CONSOLE_SEGMENT = "console";

    private final AppAuthenticationService appAuthenticationService;
    private final AppConsoleAccessValidator appConsoleAccessValidator;
    private final ApplicationGuard applicationGuard;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = pathOf(httpRequest);

        String applicationId = applicationIdOf(path);
        if(applicationId == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            applicationGuard.check(applicationId);
        }
        catch (ApplicationNotFoundException e) {
            httpResponse.sendRedirect("/error/error404");
            return;
        }

        try {
            AuthenticatedUser user = appAuthenticationService.authentication(applicationId, TokenCookie.resolveAccessToken(httpRequest));

            appConsoleAccessValidator.validate(applicationId, user.getUserId());
        }
        catch (SecurityBusinessException e) {
            String returnUrl = URLEncoder.encode("/"+applicationId+"/"+CONSOLE_SEGMENT, StandardCharsets.UTF_8);
            httpResponse.sendRedirect("/error/error401?returnUrl="+returnUrl);
            return;
        }
        catch (NotAppAdminException e) {
            httpResponse.sendRedirect("/error/error403");
            return;
        }

        chain.doFilter(request, response);
    }

    // 컨텍스트 경로를 뗀 요청 경로
    static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if(contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        return uri;
    }

    /**
     * /{applicationId}/console 또는 /{applicationId}/console/... 이면 applicationId 를, 콘솔 경로가 아니면 null 을 돌려준다.
     * 예) /ehr/console/workflow → ehr, /ehr/workflow → null, /console/console → null (플랫폼 콘솔)
     */
    static String applicationIdOf(String path) {
        if(path == null || !path.startsWith("/")) {
            return null;
        }

        int slash = path.indexOf('/', 1);
        if(slash <= 1) {
            return null;
        }

        String applicationId = path.substring(1, slash);
        if(CONSOLE_SEGMENT.equals(applicationId)) {
            return null;
        }

        String rest = path.substring(slash);
        boolean isConsole = rest.equals("/" + CONSOLE_SEGMENT) || rest.startsWith("/" + CONSOLE_SEGMENT + "/");

        return isConsole ? applicationId : null;
    }
}
