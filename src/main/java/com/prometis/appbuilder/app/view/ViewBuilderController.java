package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.security.config.SecurityProperties;
import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.app.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.app.security.service.AuthService;
import com.prometis.appbuilder.app.security.token.TokenCookie;
import com.prometis.appbuilder.app.view.code.ObjectType;
import com.prometis.appbuilder.app.view.domain.Layout;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping
public class ViewBuilderController {
    @Autowired
    LayoutService layoutService;
    @Autowired
    ViewObjectService viewObjectService;
    @Autowired
    AuthService authService;
    @Autowired
    SecurityProperties securityProperties;

    @GetMapping("")
    public String moveAppIndex(HttpServletRequest request, HttpServletResponse httpResponse) throws IOException {
        Layout layout = layoutService.getLayout();

        if(Boolean.TRUE.equals(layout.getUseAuthValidation())) {
            AuthenticatedUser user = null;
            try {
                user = authService.authentication(TokenCookie.resolveAccessToken(request));
            }
            catch (SecurityBusinessException e) {
                return "/error/error401";
            }
        }

        return "/pages/index";
    }
    @GetMapping("/pages/{objectCode}")
    public String moveAppPages(
            HttpServletRequest request,
            Model model,
            @PathVariable("objectCode") String objectCode
    ) {
        ViewObject viewObject = viewObjectService.getViewObject(objectCode);
        if(viewObject == null) {
            return "/error/error404";
        }

        if(Boolean.TRUE.equals(viewObject.getUseAuthValidation())) {
            AuthenticatedUser user;
            try {
                user = authService.authentication(TokenCookie.resolveAccessToken(request));
            } catch (SecurityBusinessException e) {
                return "/error/error401";
            }

            if(Boolean.TRUE.equals(viewObject.getUseAuthorityValidation())) {
                try {
                    viewObjectService.validateAuthorization(user, viewObject);
                }
                catch (BusinessException e) {
                    return "/error/error403";
                }
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
