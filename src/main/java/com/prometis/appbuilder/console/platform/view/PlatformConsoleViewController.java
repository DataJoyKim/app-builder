package com.prometis.appbuilder.console.platform.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/console")
public class PlatformConsoleViewController {

    @GetMapping("")
    public String moveIndex() {
        return "console/platform/index";
    }

    @GetMapping("/{path}")
    public String moveDataSource(Model model, @PathVariable(name = "path") String path) {
        model.addAttribute("contentPath","/console/platform/contents/"+path);
        return "console/platform/contents/content";
    }
}
