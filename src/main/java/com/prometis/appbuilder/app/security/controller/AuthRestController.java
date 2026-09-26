package com.prometis.appbuilder.app.security.controller;

import com.prometis.appbuilder.app.security.service.AuthService;
import com.prometis.appbuilder.app.security.token.AuthTokenResponse;
import com.prometis.appbuilder.app.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.app.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    @Autowired
    AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) throws SecurityBusinessException {
        AuthTokenResponse token = authService.refreshToken(TokenCookie.resolveRefreshToken(httpRequest));

        TokenCookie.setAccessToken(httpResponse, token.getAccessToken());
        TokenCookie.setRefreshToken(httpResponse, token.getRefreshToken());

        return new ResponseEntity<>(token, HttpStatus.OK);
    }
}
