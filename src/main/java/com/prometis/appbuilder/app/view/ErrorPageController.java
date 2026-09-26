package com.prometis.appbuilder.app.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class ErrorPageController {

    @GetMapping("/{errorPage}")
    public String moveErrorPage(@PathVariable(name = "errorPage") String errorPage) {
        return "/error/" + errorPage;
    }
}
