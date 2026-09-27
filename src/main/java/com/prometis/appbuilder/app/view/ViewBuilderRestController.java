package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.code.CodeRequest;
import com.prometis.appbuilder.app.code.CodeResponse;
import com.prometis.appbuilder.app.code.CodeService;
import com.prometis.appbuilder.app.code.CodeType;
import com.prometis.appbuilder.app.view.domain.*;
import com.prometis.appbuilder.app.view.dto.MenuDto;
import com.prometis.appbuilder.app.view.dto.ProfileDto;
import com.prometis.appbuilder.app.view.dto.ViewDto;
import com.prometis.appbuilder.platform.application.ApplicationGuard;
import com.prometis.appbuilder.platform.application.ApplicationNotFoundException;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
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
@RequestMapping("/{applicationId}")
public class ViewBuilderRestController {
    @Autowired
    MenuService menuService;
    @Autowired
    LayoutService layoutService;
    @Autowired
    ViewObjectService viewObjectService;
    @Autowired
    CodeService codeService;
    @Autowired
    ApplicationGuard applicationGuard;
    @Autowired
    ViewGuard viewGuard;

    @GetMapping("/api/menu/tree")
    public ResponseEntity<?> getMenu(
            @PathVariable("applicationId") String applicationId,
            @RequestParam("parentMenuCd") String parentMenuCd
    ) {
        List<MenuDto> menuList = menuService.getMenuTree(applicationId, parentMenuCd);

        return new ResponseEntity<>(menuList, HttpStatus.OK);
    }
    @GetMapping("/api/menu/root")
    public ResponseEntity<?> getMenuRoot(
            HttpServletRequest request,
            @PathVariable("applicationId") String applicationId
    ) {
        AuthenticatedUser user;
        try {
            user = viewGuard.check(request, applicationId);
        }
        catch (SecurityBusinessException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<MenuDto> menuList = menuService.getMenuRoot(applicationId, user);

        return new ResponseEntity<>(menuList, HttpStatus.OK);
    }
    @GetMapping("/api/layout")
    public ResponseEntity<?> getLayout(@PathVariable("applicationId") String applicationId) {
        Layout response = layoutService.getLayout(applicationId);
        if(response == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @GetMapping("/api/profile")
    public ResponseEntity<?> getProfile(
            HttpServletRequest request,
            @PathVariable("applicationId") String applicationId
    ) {
        AuthenticatedUser user;
        try {
            user = viewGuard.check(request, applicationId);
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
            @PathVariable("applicationId") String applicationId,
            @PathVariable("objectCode") String objectCode
    ) {
        try {
            applicationGuard.check(applicationId);
        }
        catch (ApplicationNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        ViewObject viewObject = viewObjectService.getViewObject(applicationId, objectCode);

        try {
            viewGuard.check(request,applicationId,viewObject);
        }
        catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        ViewObjectContent viewObjectContent = viewObjectService.getViewObjectContent(applicationId, objectCode);

        //TODO viewContent로 통합필요
        List<ViewAction> viewActions = viewObjectService.getViewActions(applicationId, objectCode);

        List<ViewCode> viewCodes = viewObjectService.getViewCodes(applicationId, objectCode);

        List<CodeRequest> params = new ArrayList<>();
        for(ViewCode viewCode : viewCodes) {
            CodeRequest codeRequest = new CodeRequest();
            codeRequest.setName(viewCode.getTarget());
            codeRequest.setType(CodeType.valueOf(viewCode.getType()));
            params.add(codeRequest);
        }

        Map<String, List<CodeResponse>> codeMap = (params.isEmpty()) ? new HashMap<>() : codeService.getCode(applicationId, params);

        return new ResponseEntity<>(ViewDto.builder()
                .viewObject(viewObject)
                .viewObjectContent(viewObjectContent)
                .viewActions(viewActions)
                .codeMap(codeMap)
                .build(), HttpStatus.OK);
    }
}
