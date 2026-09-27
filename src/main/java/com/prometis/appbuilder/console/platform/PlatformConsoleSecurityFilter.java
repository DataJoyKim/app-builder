package com.prometis.appbuilder.console.platform;

import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.token.TokenCookie;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class PlatformConsoleSecurityFilter implements Filter {
    private static final String CONSOLE_SEGMENT = "console";

    private final AuthenticationService authenticationService;
    private final PlatformConsoleAccessValidator platformConsoleAccessValidator;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            AuthenticatedUser user = authenticationService.authentication(TokenCookie.resolveAccessToken(httpRequest));

            platformConsoleAccessValidator.validate(user.getUserId());
        }
        catch (SecurityBusinessException e) {
            String returnUrl = URLEncoder.encode("/"+CONSOLE_SEGMENT, StandardCharsets.UTF_8);
            httpResponse.sendRedirect("/error/error401?returnUrl="+returnUrl);
            return;
        }
        catch (NotPlatformAdminException e) {
            httpResponse.sendRedirect("/error/error403");
            return;
        }

        chain.doFilter(request, response);
    }
}
