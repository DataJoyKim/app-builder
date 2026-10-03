package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.console.app.ApplicationManageException;
import com.prometis.appbuilder.console.app.ApplicationManageService;
import com.prometis.appbuilder.console.app.dto.ApplicationCreateRequest;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 애플리케이션 콘솔의 "Application" 메뉴. 로그인한 사용자가 소유한(관리자인) 애플리케이션을 조회하고 새로 만든다.
 * 경로의 {applicationId} 는 지금 보고 있는 콘솔일 뿐이고, 대상은 로그인한 사용자 기준이다.
 */
@RestController
@RequestMapping("/{applicationId}/console/api/application-manage")
public class ApplicationManageRestController {
    @Autowired
    private ApplicationManageService applicationManageService;
    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping("")
    public ResponseEntity<?> getList(HttpServletRequest httpRequest) {
        AuthenticatedUser user;
        try {
            user = authenticationService.authentication(TokenCookie.resolveAccessToken(httpRequest));
        }
        catch (SecurityBusinessException e) {
            return message(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return new ResponseEntity<>(applicationManageService.getOwnedApplications(user.getUserId()), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(HttpServletRequest httpRequest, @RequestBody ApplicationCreateRequest request) {
        AuthenticatedUser user;
        try {
            user = authenticationService.authentication(TokenCookie.resolveAccessToken(httpRequest));
        }
        catch (SecurityBusinessException e) {
            return message(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        try {
            Application saved = applicationManageService.create(user.getUserId(), request);
            return new ResponseEntity<>(saved, HttpStatus.OK);
        }
        catch (ApplicationManageException e) {
            return message(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private ResponseEntity<?> message(HttpStatus status, String message) {
        return new ResponseEntity<>(Map.of("message", message), status);
    }
}
