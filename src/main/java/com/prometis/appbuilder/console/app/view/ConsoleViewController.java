package com.prometis.appbuilder.console.app.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 애플리케이션 콘솔 화면. URL 은 /{applicationId}/console/** 이고, 화면은 applicationId 를 받아
 * 콘솔 API(/{applicationId}/console/api/**)를 그 애플리케이션으로 호출한다.
 */
@Controller
// applicationId 로 console 은 받지 않는다. /console/console 이 플랫폼 콘솔의 /console/{path} 와 겹치지 않게 하기 위함 (console 은 예약된 ID)
@RequestMapping("/{applicationId:(?!console$).+}/console")
public class ConsoleViewController {

    @GetMapping("")
    public String moveIndex(Model model, @PathVariable(name = "applicationId") String applicationId) {
        model.addAttribute("applicationId", applicationId);
        return "console/app/index";
    }

    @GetMapping("/{path}")
    public String moveDataSource(
            Model model,
            @PathVariable(name = "applicationId") String applicationId,
            @PathVariable(name = "path") String path
    ) {
        model.addAttribute("applicationId", applicationId);
        model.addAttribute("contentPath","/console/app/contents/"+path);
        return "console/app/contents/content";
    }
}
