package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * 애플리케이션 가입 페이지 (/{applicationId}/signup). 애플리케이션 관리자가 사용자에게 전달하는 링크다.
 * 가입 방식(콘솔 사용자 화면의 가입 설정)에 따라 동작한다.
 * - 초대만(INVITE_ONLY) : 가입할 수 없다는 안내만 보여준다 (기본값)
 * - 승인(APPROVAL) : 가입 신청을 남기고, 관리자가 승인하면 사용자가 된다
 * - 자유(OPEN) : 바로 사용자가 된다
 * 계정이 없으면 메일 인증으로 계정을 만들면서 가입하고, 이미 계정이 있으면 로그인한 상태로 가입한다.
 * 플랫폼 공개 가입(/signup)과 달리 그 애플리케이션의 사용자로 들어온다.
 */
@Controller
// console 은 예약된 ID 라 받지 않는다. /console/signup 이 플랫폼 콘솔의 /console/{path} 와 겹치지 않게 하기 위함
@RequestMapping("/{applicationId:(?!console$).+}")
public class AppSignupController {
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JoinService joinService;
    @Autowired
    AppMembershipService appMembershipService;
    @Autowired
    JoinLogin joinLogin;
    @Autowired
    JoinProperties joinProperties;

    @GetMapping("/signup")
    public String signup(HttpServletRequest request, Model model, @PathVariable("applicationId") String applicationId) {
        Optional<Application> application = applicationRepository.findByApplicationId(applicationId);
        if(application.isEmpty()) {
            return "/error/error404";
        }

        AppJoinPolicy policy = appMembershipService.policyOf(applicationId);

        model.addAttribute("mode", "APP_SIGNUP");
        model.addAttribute("applicationId", applicationId);
        model.addAttribute("applicationName", application.get().getName());
        model.addAttribute("apiBase", "/" + applicationId + "/api/signup");
        model.addAttribute("joinPolicy", policy.name());
        model.addAttribute("inviteOnlyMessage", AppMembershipService.INVITE_ONLY_MESSAGE);
        model.addAttribute("codeTtlText", JoinMailSender.describe(joinProperties.verificationCodeTtl()));

        Long loginUserId = joinLogin.loginUserIdOf(request);
        User loginUser = loginUserId == null ? null : userRepository.findById(loginUserId).orElse(null);
        if(loginUser != null) {
            model.addAttribute("loginUserName", loginUser.getUserName());
            model.addAttribute("loginUserEmail", loginUser.getEmail());
            model.addAttribute("appJoinStatus", appMembershipService.statusOf(applicationId, loginUser.getId()).name());
        }

        return "console/app/join";
    }

    /** 새 계정: 인증코드 메일 */
    @PostMapping("/api/signup/request")
    @ResponseBody
    public ResponseEntity<?> request(@PathVariable("applicationId") String applicationId, @RequestBody JoinRequest request) {
        try {
            return new ResponseEntity<>(joinService.requestAppSignupCode(applicationId, request), HttpStatus.OK);
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    /** 새 계정: 인증코드 확인 → 계정 생성 + 로그인 + 가입/신청 */
    @PostMapping("/api/signup/verify")
    @ResponseBody
    public ResponseEntity<?> verify(
            @PathVariable("applicationId") String applicationId,
            @RequestBody JoinVerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        try {
            JoinService.AppSignupResult result = joinService.verifyAppSignup(applicationId, request);
            joinLogin.login(result.user(), httpRequest, httpResponse);

            return done(applicationId, result.status());
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    /** 기존 계정: 로그인한 상태로 가입/신청 */
    @PostMapping("/api/signup/apply")
    @ResponseBody
    public ResponseEntity<?> apply(@PathVariable("applicationId") String applicationId, HttpServletRequest httpRequest) {
        Long loginUserId = joinLogin.loginUserIdOf(httpRequest);
        if(loginUserId == null) {
            return new ResponseEntity<>(Map.of("message", "로그인이 필요합니다."), HttpStatus.UNAUTHORIZED);
        }
        if(applicationRepository.findByApplicationId(applicationId).isEmpty()) {
            return badRequest("존재하지 않는 애플리케이션입니다.");
        }

        try {
            return done(applicationId, appMembershipService.applyForApplication(applicationId, loginUserId));
        }
        catch (JoinException e) {
            return badRequest(e.getMessage());
        }
    }

    // 가입했으면 애플리케이션으로, 신청했으면 이 페이지(승인 대기 안내)로, 가입하지 못했으면 루트로
    private ResponseEntity<?> done(String applicationId, AppJoinStatus status) {
        String redirectUrl;
        String message;
        switch (status) {
            case MEMBER:
                redirectUrl = "/" + applicationId;
                message = "가입이 완료되었습니다.";
                break;
            case PENDING:
                redirectUrl = "/" + applicationId + "/signup";
                message = "가입을 신청했습니다. 관리자가 승인하면 이용할 수 있습니다.";
                break;
            default:
                redirectUrl = "/";
                message = "계정은 만들어졌지만, 이 애플리케이션은 지금 초대받은 사람만 가입할 수 있습니다.";
        }

        return new ResponseEntity<>(Map.of("status", status.name(), "redirectUrl", redirectUrl, "message", message), HttpStatus.OK);
    }

    private ResponseEntity<?> badRequest(String message) {
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }
}
