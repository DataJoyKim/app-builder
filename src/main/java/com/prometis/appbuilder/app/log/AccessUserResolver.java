package com.prometis.appbuilder.app.log;

import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 로그에 남길 "누가". 인증을 요구하지 않는 워크플로우/화면은 가드가 사용자를 돌려주지 않으므로,
 * 로그인 쿠키가 있으면 토큰만 확인해서 사용자를 찾는다. 세션 등록이나 접속 시각 기록 같은 부수효과는 없다.
 * 토큰이 없거나 유효하지 않으면 익명(null)이다.
 */
@Component
@RequiredArgsConstructor
public class AccessUserResolver {
    private final AuthenticationService authenticationService;

    public AuthenticatedUser resolve(HttpServletRequest request, AuthenticatedUser known) {
        if(known != null || request == null) {
            return known;
        }

        String accessToken = TokenCookie.resolveAccessToken(request);
        if(accessToken == null) {
            return null;
        }

        try {
            return authenticationService.authentication(accessToken);
        }
        catch (Exception e) {
            return null;
        }
    }
}
