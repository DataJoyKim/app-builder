package com.prometis.appbuilder.security.controller;

import com.prometis.appbuilder.app.view.LayoutService;
import com.prometis.appbuilder.app.view.domain.Layout;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

/**
 * 애플리케이션 로그인 화면 (/{applicationId}/login). 일반 사용자가 애플리케이션에 접속할 때 쓴다.
 * 로고는 Layout.loginImgLogoLink 이미지, 설정이 없으면 애플리케이션 이름을 글자로 보여주고,
 * 로그인하면 애플리케이션(/{applicationId})으로 간다.
 * 플랫폼 로그인(/login)과 같은 로그인 API(/api/login)를 쓴다.
 */
@Controller
public class AppLoginViewController {
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    LayoutService layoutService;

    // console 은 예약된 ID 라 받지 않는다. /console/login 이 플랫폼 콘솔의 /console/{path} 와 겹치지 않게 하기 위함
    @GetMapping("/{applicationId:(?!console$).+}/login")
    public String login(Model model, @PathVariable("applicationId") String applicationId) {
        Optional<Application> application = applicationRepository.findByApplicationId(applicationId);
        if(application.isEmpty()) {
            return "error/error404";
        }

        Layout layout = layoutService.getLayout(applicationId);

        model.addAttribute("applicationId", applicationId);
        model.addAttribute("applicationName", application.get().getName());
        model.addAttribute("loginImgLogoLink", blankToNull(layout.getLoginImgLogoLink()));
        model.addAttribute("faviconPath", blankToNull(layout.getFaviconPath()));

        return "login/app-login";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
