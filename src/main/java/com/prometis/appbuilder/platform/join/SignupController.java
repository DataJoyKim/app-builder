package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.platform.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 공개 가입. 누구나 /signup 에서 메일 인증을 거쳐 계정을 만든다 (홍보용으로 공개해도 남의 애플리케이션 권한은 생기지 않는다).
 * 가입을 마치면 애플리케이션 관리자(users.authority = APPLICATION_ADMIN)가 되고, 로그인된 상태로 루트(/)로 가서 애플리케이션 선택 화면으로 이어진다.
 */
@Controller
public class SignupController {
    @Autowired
    JoinService joinService;
    @Autowired
    JoinLogin joinLogin;
    @Autowired
    JoinProperties joinProperties;

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("mode", "SIGNUP");
        model.addAttribute("apiBase", "/api/signup");
        model.addAttribute("codeTtlText", JoinMailSender.describe(joinProperties.verificationCodeTtl()));

        return "console/app/join";
    }

    @PostMapping("/api/signup/request")
    @ResponseBody
    public ResponseEntity<?> request(@RequestBody JoinRequest request) {
        try {
            return new ResponseEntity<>(joinService.requestSignupCode(request), HttpStatus.OK);
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/api/signup/verify")
    @ResponseBody
    public ResponseEntity<?> verify(
            @RequestBody JoinVerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        try {
            User user = joinService.verifySignup(request);
            joinLogin.login(user, httpRequest, httpResponse);

            return new ResponseEntity<>(Map.of("redirectUrl", "/"), HttpStatus.OK);
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    private ResponseEntity<?> badRequest(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }
}
