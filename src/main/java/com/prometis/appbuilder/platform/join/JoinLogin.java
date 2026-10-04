package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.security.domain.Client;
import com.prometis.appbuilder.security.dto.AuthTokenResponse;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.service.LoginService;
import com.prometis.appbuilder.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 가입 화면/API 에서 쓰는 로그인 처리: 가입을 마친 사람을 바로 로그인시키고, 지금 로그인한 사람을 확인한다.
 */
@Component
@RequiredArgsConstructor
class JoinLogin {
    private final LoginService loginService;
    private final AuthenticationService authenticationService;

    void login(User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthTokenResponse token = loginService.issueTokens(user, new Client(httpRequest));
        TokenCookie.setAccessToken(httpResponse, token.getAccessToken());
        TokenCookie.setRefreshToken(httpResponse, token.getRefreshToken());
    }

    /** 로그인한 사용자의 userId, 로그인하지 않았으면 null */
    Long loginUserIdOf(HttpServletRequest httpRequest) {
        try {
            return authenticationService.authentication(TokenCookie.resolveAccessToken(httpRequest)).getUserId();
        }
        catch (SecurityBusinessException e) {
            return null;
        }
    }
}
