package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.code.ObjectType;
import com.prometis.appbuilder.app.view.domain.Layout;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.appbuilder.platform.application.ApplicationGuard;
import com.prometis.appbuilder.platform.application.ApplicationNotFoundException;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{applicationId}")
public class ViewBuilderController {
    @Autowired
    LayoutService layoutService;
    @Autowired
    ViewObjectService viewObjectService;
    @Autowired
    ViewGuard viewGuard;
    @Autowired
    ApplicationGuard applicationGuard;

    @GetMapping("")
    public String moveAppIndex(
            HttpServletRequest request,
            HttpServletResponse httpResponse,
            Model model,
            @PathVariable("applicationId") String applicationId
    ) {
        try {
            applicationGuard.check(applicationId);
        }
        catch (ApplicationNotFoundException e) {
            return "/error/error404";
        }

        Layout layout = layoutService.getLayout(applicationId);

        try {
            viewGuard.check(request, applicationId, layout);
        }
        catch (BusinessException e) {
            if(e.getStatus() == 401) {
                return "/error/error401";
            }
            else {
                return "/error/error403";
            }
        }

        // 브라우저 탭 아이콘은 첫 HTML 에 있어야 하므로 서버에서 렌더링한다
        if(layout.getFaviconPath() != null && !layout.getFaviconPath().isBlank()) {
            model.addAttribute("faviconPath", layout.getFaviconPath().trim());
        }

        return "/pages/index";
    }
    @GetMapping("/pages/{objectCode}")
    public String moveAppPages(
            HttpServletRequest request,
            Model model,
            @PathVariable("applicationId") String applicationId,
            @PathVariable("objectCode") String objectCode
    ) {
        try {
            applicationGuard.check(applicationId);
        }
        catch (ApplicationNotFoundException e) {
            return "/error/error404";
        }

        ViewObject viewObject = viewObjectService.getViewObject(applicationId, objectCode);
        if(viewObject == null) {
            return "/error/error404";
        }

        try {
            viewGuard.check(request, applicationId, viewObject);
        }
        catch (BusinessException e) {
            if(e.getStatus() == 401) {
                return "/error/error401";
            }
            else {
                return "/error/error403";
            }
        }

        model.addAttribute("objectPath","/pages" + viewObject.getPath());
        model.addAttribute("objectCode",objectCode);

        if(ObjectType.FILE.equals(viewObject.getType())) {
            return "/template/mf-template";
        }
        else if(ObjectType.VIEW_BUILDER.equals(viewObject.getType())) {
            return "/template/view-builder-template";
        }
        else {
            return null;
        }
    }
}
