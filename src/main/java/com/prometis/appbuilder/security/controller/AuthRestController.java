package com.prometis.appbuilder.security.controller;

import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.dto.AuthTokenResponse;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    @Autowired
    AuthenticationService authenticationService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) throws SecurityBusinessException {
        AuthTokenResponse token = authenticationService.refreshToken(TokenCookie.resolveRefreshToken(httpRequest));

        TokenCookie.setAccessToken(httpResponse, token.getAccessToken());
        TokenCookie.setRefreshToken(httpResponse, token.getRefreshToken());

        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse httpResponse) {
        TokenCookie.clear(httpResponse);

        // HttpClient.post 는 JSON 응답을 기대하므로 빈 객체를 돌려준다
        return new ResponseEntity<>(Map.of(), HttpStatus.OK);
    }
}
