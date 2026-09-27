package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.domain.GrantedPermission;
import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.dto.MenuDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuRepository menuRepository;
    private final MenuPermissionRepository menuPermissionRepository;

    public List<MenuDto> getMenuTree(String applicationId, String parentMenuCd) {
        Optional<Menu> parentMenu = menuRepository.findByApplicationIdAndMenuCd(applicationId, parentMenuCd);
        if(parentMenu.isEmpty()) {
            // 이 애플리케이션에 없는 메뉴코드면 보여줄 하위 메뉴가 없다.
            return new ArrayList<>();
        }

        List<Menu> menuList = menuRepository.findAllTree(applicationId, parentMenu.get());

        menuList.forEach(Menu::processing);

        return MenuDto.of(menuList, applicationId);
    }

    public List<MenuDto> getMenuRoot(String applicationId, AuthenticatedUser user) {
        List<GrantedPermission> grantedPermissions = user.getGrantedPermissions();
        if(grantedPermissions == null || grantedPermissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 권한이 여러 개면 같은 메뉴가 여러 번 걸릴 수 있어 한 번만 담는다(처음 나온 순서 유지).
        Map<Long, Menu> menus = new LinkedHashMap<>();
        for(GrantedPermission grantedPermission : grantedPermissions) {
            List<MenuPermission> menuPermissions =
                    menuPermissionRepository.findByPermissionCodeAndMenu_ApplicationId(grantedPermission.getRole(), applicationId);

            for(MenuPermission menuPermission : menuPermissions) {
                Menu menu = menuPermission.getMenu();
                menus.putIfAbsent(menu.getId(), menu);
            }
        }

        List<Menu> menuList = new ArrayList<>(menus.values());

        menuList.forEach(Menu::processing);

        return MenuDto.of(menuList, applicationId);
    }
}
