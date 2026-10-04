package com.prometis.appbuilder.platform.home;

import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserService;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.service.AuthenticationService;
import com.prometis.appbuilder.security.token.TokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 루트(/) 진입 화면.
 * 로그인하지 않았으면 /login 으로, 로그인했으면 사용자 유형(HomeLanding)에 맞는 첫 화면으로 보낸다.
 * /applications, /applications/manage 는 최상위 경로라 platform.application.reserved-ids 에 applications 를 등록해 두었다.
 */
@Controller
public class HomeViewController {
    @Autowired
    AuthenticationService authenticationService;
    @Autowired
    UserService userService;
    @Autowired
    HomeService homeService;

    @GetMapping("/")
    public String home(HttpServletRequest request) {
        AuthenticatedUser user = authenticatedUserOf(request);
        if(user == null) {
            return "redirect:/login";
        }

        return "redirect:" + homeService.landingOf(user.getUserId()).getPath();
    }

    /** 일반 사용자: 가입된 애플리케이션을 골라 접속한다 (계정 선택 화면과 같은 형태) */
    @GetMapping("/applications")
    public String applicationSelect(HttpServletRequest request, Model model) {
        AuthenticatedUser user = authenticatedUserOf(request);
        if(user == null) {
            return redirectLogin(HomeLanding.APPLICATION_SELECT);
        }

        User loginUser = userService.getUserByUserId(user.getUserId());

        model.addAttribute("userName", loginUser.getUserName());
        model.addAttribute("loginId", loginUser.getLoginId());
        model.addAttribute("email", loginUser.getEmail());
        model.addAttribute("applications", homeService.getJoinedApplications(user.getUserId()));

        return "home/application-select";
    }

    /**
     * 소유한 애플리케이션에 접속하거나 새로 만든다 (애플리케이션 콘솔의 application-manage 화면을 그대로 쓴다).
     * 공개 가입으로 누구나 계정을 만들 수 있으므로 로그인한 사용자면 누구나 들어올 수 있다.
     */
    @GetMapping("/applications/manage")
    public String applicationManage(HttpServletRequest request, Model model) {
        AuthenticatedUser user = authenticatedUserOf(request);
        if(user == null) {
            return redirectLogin(HomeLanding.APPLICATION_MANAGE);
        }

        model.addAttribute("userName", user.getUserName());

        return "home/application-manage";
    }

    private AuthenticatedUser authenticatedUserOf(HttpServletRequest request) {
        try {
            return authenticationService.authentication(TokenCookie.resolveAccessToken(request));
        }
        catch (SecurityBusinessException e) {
            return null;
        }
    }

    private String redirectLogin(HomeLanding landing) {
        return "redirect:/login?returnUrl=" + URLEncoder.encode(landing.getPath(), StandardCharsets.UTF_8);
    }
}
