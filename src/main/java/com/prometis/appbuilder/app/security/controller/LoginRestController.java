package com.prometis.appbuilder.app.security.controller;

import com.prometis.appbuilder.app.security.domain.Client;
import com.prometis.appbuilder.app.security.dto.LoginRequest;
import com.prometis.appbuilder.app.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.app.security.service.LoginService;
import com.prometis.appbuilder.app.security.token.AuthTokenResponse;
import com.prometis.appbuilder.app.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login")
public class LoginRestController {
    @Autowired
    LoginService loginService;

    @PostMapping("")
    public ResponseEntity<?> login(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @RequestBody LoginRequest loginRequest
    ) throws SecurityBusinessException {
        Client client = new Client(httpRequest);

        AuthTokenResponse token = loginService.login(loginRequest, client);

        TokenCookie.setAccessToken(httpResponse, token.getAccessToken());
        TokenCookie.setRefreshToken(httpResponse, token.getRefreshToken());

        return new ResponseEntity<>(token, HttpStatus.OK);
    }
}
