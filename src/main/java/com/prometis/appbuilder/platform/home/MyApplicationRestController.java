package com.prometis.appbuilder.platform.home;

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
 * 애플리케이션 콘솔 밖(/applications/manage)에서 쓰는 소유 애플리케이션 조회/생성 API.
 * 애플리케이션 콘솔의 /{applicationId}/console/api/application-manage 와 같은 동작이지만,
 * 아직 들어갈 콘솔이 정해지지 않은 상태라 applicationId 없이 로그인한 사용자 기준으로만 동작한다.
 * 공개 가입(/signup)으로 누구나 계정을 만들 수 있으므로 로그인한 사용자면 누구나 쓸 수 있다 (소유 개수는 max-owned-count 로 제한).
 */
@RestController
@RequestMapping("/api/my-applications")
public class MyApplicationRestController {
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
