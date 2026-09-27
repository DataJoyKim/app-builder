package com.prometis.appbuilder.security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginViewController {

    @GetMapping("/login")
    public String login(
            Model model,
            @RequestParam(name = "returnUrl", required = false) String returnUrl
    ) {
        model.addAttribute("returnUrl",returnUrl);
        return "login/login";
    }
}
