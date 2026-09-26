package com.prometis.appbuilder.app.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserViewController {

    @GetMapping("/signup")
    public String login() {
        return "login/signup";
    }
}
