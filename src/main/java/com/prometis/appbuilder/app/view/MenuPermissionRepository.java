package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuPermissionRepository extends JpaRepository<MenuPermission, Long> {
    List<MenuPermission> findByMenu(Menu menu);

    List<MenuPermission> findByPermissionCode(String permissionCode);

    // 권한이 걸린 메뉴 중 그 애플리케이션의 메뉴만
    List<MenuPermission> findByPermissionCodeAndMenu_ApplicationId(String permissionCode, String applicationId);
}
