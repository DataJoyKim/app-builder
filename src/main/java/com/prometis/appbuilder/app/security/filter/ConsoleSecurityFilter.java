package com.prometis.appbuilder.app.security.filter;

import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.app.security.domain.GrantedAuthority;
import com.prometis.appbuilder.app.security.config.SecurityProperties;
import com.prometis.appbuilder.app.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.app.security.service.AuthService;
import com.prometis.appbuilder.app.security.token.TokenCookie;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class ConsoleSecurityFilter implements Filter {
    private final AuthService authService;
    private final SecurityProperties securityProperties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        AuthenticatedUser user;
        try {
            user = authService.authentication(TokenCookie.resolveAccessToken((HttpServletRequest) request));
        }
        catch (SecurityBusinessException e) {
            String returnUrl = URLEncoder.encode("/console", StandardCharsets.UTF_8);
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendRedirect("/error/error401?returnUrl="+returnUrl);
            return;
        }

        if(!GrantedAuthority.hasAuthority(user.getGrantedAuthorities(), securityProperties.getConsoleAccessPermitAuthorityCode())) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendRedirect("/error/error403");
            return;
        }

        chain.doFilter(request, response);
    }
}
