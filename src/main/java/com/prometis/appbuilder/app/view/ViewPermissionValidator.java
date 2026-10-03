package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.domain.GrantedPermission;
import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViewPermissionValidator {
    private final MenuRepository menuRepository;
    private final MenuPermissionRepository menuPermissionRepository;

    public void validate(AuthenticatedUser user, ViewObject accessViewObject) throws BusinessException {
        List<GrantedPermission> grantedPermissionList = user.getGrantedPermissions();
        if(grantedPermissionList == null || grantedPermissionList.isEmpty()) {
            throw new BusinessException(ViewErrorMessage.NOT_HAS_PERMISSIONS);
        }

        List<Menu> menuList = menuRepository.findByViewObject(accessViewObject);

        // 상위 메뉴 가져오기
        List<Menu> rootMenuList = new ArrayList<>();
        for(Menu menu : menuList) {
            Menu rootMenu = menu;
            while (rootMenu.getParentMenu() != null) {
                rootMenu = rootMenu.getParentMenu();
            }

            rootMenuList.add(rootMenu);
        }

        // ROOT 메뉴에 매핑된 권한 가져오기
        Map<String, MenuPermission> menuPermissionMap = new HashMap<>();
        for(Menu rootMenu : rootMenuList) {
            List<MenuPermission> menuPermissions = menuPermissionRepository.findByMenu(rootMenu);

            for(MenuPermission menuPermission : menuPermissions) {
                menuPermissionMap.put(menuPermission.getPermissionCode(), menuPermission);
            }
        }

        if(menuPermissionMap.isEmpty()) {
            throw new BusinessException(ViewErrorMessage.NOT_SETTING_PERMISSION);
        }

        // 내가 가진 권한 검증
        boolean hasPermission = false;
        for(GrantedPermission grantedPermission : grantedPermissionList) {
            if(menuPermissionMap.containsKey(grantedPermission.getRole())) {
                hasPermission = true;
                break;
            }
        }

        if(!hasPermission) {
            throw new BusinessException(ViewErrorMessage.PERMISSION_DENIED);
        }
    }
}
