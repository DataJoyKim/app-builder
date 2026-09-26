package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.code.CodeRequest;
import com.prometis.appbuilder.app.code.CodeResponse;
import com.prometis.appbuilder.app.code.CodeService;
import com.prometis.appbuilder.app.code.CodeType;
import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.app.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.app.security.service.AuthService;
import com.prometis.appbuilder.app.security.token.TokenCookie;
import com.prometis.appbuilder.app.view.domain.*;
import com.prometis.appbuilder.app.view.dto.MenuDto;
import com.prometis.appbuilder.app.view.dto.ProfileDto;
import com.prometis.appbuilder.app.view.dto.ViewDto;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class ViewBuilderRestController {
    @Autowired
    MenuService menuService;
    @Autowired
    LayoutService layoutService;
    @Autowired
    AuthService authService;
    @Autowired
    ViewObjectService viewObjectService;
    @Autowired
    CodeService codeService;

    @GetMapping("/api/menu/tree")
    public ResponseEntity<?> getMenu(@RequestParam("parentMenuCd") String parentMenuCd) {
        List<MenuDto> menuList = menuService.getMenuTree(parentMenuCd);

        return new ResponseEntity<>(menuList, HttpStatus.OK);
    }
    @GetMapping("/api/menu/root")
    public ResponseEntity<?> getMenuRoot(HttpServletRequest request) {
        AuthenticatedUser user;
        try {
            user = authService.authentication(TokenCookie.resolveAccessToken(request));
        }
        catch (SecurityBusinessException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<MenuDto> menuList = menuService.getMenuRoot(user);

        return new ResponseEntity<>(menuList, HttpStatus.OK);
    }
    @GetMapping("/api/layout")
    public ResponseEntity<?> getLayout() {
        Layout response = layoutService.getLayout();
        if(response == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @GetMapping("/api/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        AuthenticatedUser user;
        try {
            user = authService.authentication(TokenCookie.resolveAccessToken(request));
        }
        catch (SecurityBusinessException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        ProfileDto.ProfileResponse response = ProfileDto.ProfileResponse.of(user);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/pages/{objectCode}/definition")
    public ResponseEntity<?> getPageDefinition(
            HttpServletRequest request,
            @PathVariable("objectCode") String objectCode
    ) {
        ViewObject viewObject = viewObjectService.getViewObject(objectCode);

        if(Boolean.TRUE.equals(viewObject.getUseAuthValidation())) {
            AuthenticatedUser user;
            try {
                user = authService.authentication(TokenCookie.resolveAccessToken(request));
            } catch (SecurityBusinessException e) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            if(Boolean.TRUE.equals(viewObject.getUseAuthorityValidation())) {
                try {
                    viewObjectService.validateAuthorization(user, viewObject);
                }
                catch (BusinessException e) {
                    return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                }
            }
        }

        ViewObjectContent viewObjectContent = viewObjectService.getViewObjectContent(objectCode);

        List<ViewAction> viewActions = viewObjectService.getViewActions(objectCode);

        List<ViewCode> viewCodes = viewObjectService.getViewCodes(objectCode);

        List<CodeRequest> params = new ArrayList<>();
        for(ViewCode viewCode : viewCodes) {
            CodeRequest codeRequest = new CodeRequest();
            codeRequest.setName(viewCode.getTarget());
            codeRequest.setType(CodeType.valueOf(viewCode.getType()));
            params.add(codeRequest);
        }

        Map<String, List<CodeResponse>> codeMap = (params.isEmpty()) ? new HashMap<>() : codeService.getCode(params);

        return new ResponseEntity<>(ViewDto.builder()
                .viewObject(viewObject)
                .viewObjectContent(viewObjectContent)
                .viewActions(viewActions)
                .codeMap(codeMap)
                .build(), HttpStatus.OK);
    }
}
