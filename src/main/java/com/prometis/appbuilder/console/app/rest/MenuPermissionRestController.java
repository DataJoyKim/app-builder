package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.view.MenuPermission;
import com.prometis.appbuilder.app.view.MenuPermissionRepository;
import com.prometis.appbuilder.app.view.MenuRepository;
import com.prometis.appbuilder.app.view.domain.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.MenuPermissionRestController")
@RequestMapping("/{applicationId}/console/api/menu-permission")
public class MenuPermissionRestController {
    @Autowired
    private MenuPermissionRepository repository;
    @Autowired
    private MenuRepository menuRepository;

    @GetMapping("")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @RequestParam Map<String,Object> params) {

        String permissionCode = (String) params.get("permissionCode");

        // 엔티티를 그대로 내보내면 Menu.children ↔ Menu.parentMenu 가 서로를 끝없이 직렬화한다(JSON 깊이 초과). 화면에 필요한 값만 담는다
        List<MenuPermissionResponse> results = repository.findByPermissionCodeAndMenu_ApplicationId(permissionCode, applicationId).stream()
                .map(MenuPermissionResponse::of)
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    public record MenuPermissionResponse(Long id, String permissionCode, String menuCd, String menuNm) {
        static MenuPermissionResponse of(MenuPermission menuPermission) {
            Menu menu = menuPermission.getMenu();
            return new MenuPermissionResponse(
                    menuPermission.getId(),
                    menuPermission.getPermissionCode(),
                    menu == null ? null : menu.getMenuCd(),
                    menu == null ? null : menu.getMenuNm());
        }
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        String menuCd = (String) params.get("menuCd");

        Menu menu = menuRepository.findByApplicationIdAndMenuCd(applicationId, menuCd)
                .orElseThrow();

        MenuPermission createdData = MenuPermission.builder()
                .permissionCode((String) params.get("permissionCode"))
                .menu(menu)
                .build();

        repository.save(createdData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        MenuPermission savedData = repository.findById(id)
                .filter(owned -> owned.getMenu() != null && applicationId.equals(owned.getMenu().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        String menuCd = (String) params.get("menuCd");

        Menu menu = menuRepository.findByApplicationIdAndMenuCd(applicationId, menuCd)
                .orElseThrow();

        savedData.update(
                (String) params.get("permissionCode"),
                menu
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        MenuPermission savedData = repository.findById(id)
                .filter(owned -> owned.getMenu() != null && applicationId.equals(owned.getMenu().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
