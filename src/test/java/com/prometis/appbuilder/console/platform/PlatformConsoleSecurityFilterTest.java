package com.prometis.appbuilder.console.platform;

import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.exception.SecurityErrorMessage;
import com.prometis.appbuilder.security.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlatformConsoleSecurityFilterTest {
    private AuthenticationService authenticationService;
    private PlatformConsoleAccessValidator platformConsoleAccessValidator;
    private PlatformConsoleSecurityFilter filter;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        platformConsoleAccessValidator = mock(PlatformConsoleAccessValidator.class);
        filter = new PlatformConsoleSecurityFilter(authenticationService, platformConsoleAccessValidator);
    }

    private MockFilterChain run(MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/console/application"), response, chain);
        return chain;
    }

    @Test
    public void 플랫폼관리자는_통과한다() throws Exception {
        when(authenticationService.authentication(any())).thenReturn(AuthenticatedUser.builder()
                .userId(7L)
                .userName("sysadmin")
                .grantedPermissions(List.of())
                .build());

        MockFilterChain chain = run(new MockHttpServletResponse());

        assertNotNull(chain.getRequest());
        verify(platformConsoleAccessValidator).validate(7L);
    }

    @Test
    public void 로그인하지_않으면_플랫폼_콘솔로_돌아오는_401로_보낸다() throws Exception {
        when(authenticationService.authentication(any())).thenThrow(new SecurityBusinessException(SecurityErrorMessage.NOT_LOGIN));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(response);

        assertNull(chain.getRequest());
        assertEquals("/error/error401?returnUrl=%2Fconsole", response.getRedirectedUrl());
        verifyNoInteractions(platformConsoleAccessValidator);
    }

    @Test
    public void 플랫폼관리자가_아니면_403() throws Exception {
        when(authenticationService.authentication(any())).thenReturn(AuthenticatedUser.builder()
                .userId(8L)
                .userName("user")
                .grantedPermissions(List.of())
                .build());
        doThrow(new NotPlatformAdminException()).when(platformConsoleAccessValidator).validate(8L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(response);

        assertNull(chain.getRequest());
        assertEquals("/error/error403", response.getRedirectedUrl());
    }
}
