package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.platform.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 애플리케이션 관리자 초대 합류 API. 계정이 없는 사람도 쓰므로 AppConsoleSecurityFilter 가 로그인을 검사하지 않는다.
 * - POST request : 초대 token + 가입 정보 → 초대받은 이메일로 인증코드 발송 (다시 부르면 재전송)
 * - POST verify  : 인증코드 확인 → 계정 생성 + 관리자 지정 + 로그인
 * - POST accept  : 이미 계정이 있는 사람이 로그인한 상태로 초대 수락
 * 성공하면 화면은 루트(/)로 이동한다.
 */
@RestController
@RequestMapping("/{applicationId}/console/api/join")
public class JoinRestController {
    @Autowired
    JoinService joinService;
    @Autowired
    JoinLogin joinLogin;

    @PostMapping("/request")
    public ResponseEntity<?> request(@PathVariable("applicationId") String applicationId, @RequestBody JoinRequest request) {
        try {
            return new ResponseEntity<>(joinService.requestInvitationCode(applicationId, request.getToken(), request), HttpStatus.OK);
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @PathVariable("applicationId") String applicationId,
            @RequestBody JoinVerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        try {
            User user = joinService.verifyInvitation(applicationId, request);
            joinLogin.login(user, httpRequest, httpResponse);

            return done();
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(
            @PathVariable("applicationId") String applicationId,
            @RequestBody JoinAcceptRequest request,
            HttpServletRequest httpRequest
    ) {
        Long loginUserId = joinLogin.loginUserIdOf(httpRequest);
        if(loginUserId == null) {
            return new ResponseEntity<>(Map.of("message", "로그인이 필요합니다."), HttpStatus.UNAUTHORIZED);
        }

        try {
            joinService.acceptInvitation(applicationId, request.getToken(), loginUserId);

            return done();
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    private ResponseEntity<?> done() {
        return new ResponseEntity<>(Map.of("redirectUrl", "/"), HttpStatus.OK);
    }

    private ResponseEntity<?> badRequest(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }
}
