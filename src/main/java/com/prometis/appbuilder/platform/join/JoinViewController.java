package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * 애플리케이션 초대 합류 화면 (/{applicationId}/console/join?token=...). 콘솔의 사용자 화면에서 만든 초대 링크다 (관리자/사용자 초대).
 * 계정이 없는 사람도 열기 때문에 AppConsoleSecurityFilter 가 로그인을 검사하지 않는다.
 * (ConsoleViewController 의 /{applicationId}/console/{path} 보다 구체적인 경로라 이쪽이 먼저 잡힌다)
 * - 초대를 쓸 수 없으면(없음/만료/사용됨) 이유만 보여준다
 * - 로그인한 상태면 초대받은 이메일의 계정인지에 따라 수락 버튼 또는 안내를 보여준다
 * - 로그인하지 않았으면 초대받은 이메일로 가입하거나, 기존 계정으로 로그인하게 한다
 */
@Controller
@RequestMapping("/{applicationId:(?!console$).+}/console")
public class JoinViewController {
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AppInvitationService appInvitationService;
    @Autowired
    JoinLogin joinLogin;
    @Autowired
    JoinProperties joinProperties;

    @GetMapping("/join")
    public String join(
            HttpServletRequest request,
            Model model,
            @PathVariable("applicationId") String applicationId,
            @RequestParam(name = "token", required = false) String token
    ) {
        Optional<Application> application = applicationRepository.findByApplicationId(applicationId);
        if(application.isEmpty()) {
            return "/error/error404";
        }

        model.addAttribute("mode", "INVITE");
        model.addAttribute("applicationId", applicationId);
        model.addAttribute("applicationName", application.get().getName());
        model.addAttribute("apiBase", "/" + applicationId + "/console/api/join");
        model.addAttribute("token", token);
        model.addAttribute("codeTtlText", JoinMailSender.describe(joinProperties.verificationCodeTtl()));

        AppInvitation invitation;
        try {
            invitation = appInvitationService.usableInvitation(applicationId, token);
        }
        catch (JoinException e) {
            model.addAttribute("invitationError", e.getMessage());
            return "console/app/join";
        }
        model.addAttribute("invitationEmail", invitation.getEmail());
        model.addAttribute("invitationRoleLabel", AppInvitationService.roleLabelOf(invitation));

        Long loginUserId = joinLogin.loginUserIdOf(request);
        User loginUser = loginUserId == null ? null : userRepository.findById(loginUserId).orElse(null);
        if(loginUser != null) {
            model.addAttribute("loginUserName", loginUser.getUserName());
            model.addAttribute("loginUserEmail", loginUser.getEmail());
            model.addAttribute("loginEmailMatches", invitation.isEmailOf(loginUser.getEmail()));
        }

        return "console/app/join";
    }
}
