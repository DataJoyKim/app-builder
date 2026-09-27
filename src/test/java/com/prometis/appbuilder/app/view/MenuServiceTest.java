package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.domain.GrantedPermission;
import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.dto.MenuDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 메뉴 트리는 시작점(부모/루트)만 applicationId 로 찾고, 그 아래는 children 을 따라가되 다른 애플리케이션 메뉴는 걸러낸다.
 */
@DataJpaTest
@Import(MenuService.class)
class MenuServiceTest {
    @Autowired
    private MenuService menuService;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private MenuPermissionRepository menuPermissionRepository;
    @Autowired
    private EntityManager entityManager;

    private Menu menu(String applicationId, String menuCd, Integer orderNum, Menu parent) {
        return menuRepository.save(Menu.builder()
                .applicationId(applicationId)
                .menuCd(menuCd)
                .menuNm(applicationId + "-" + menuCd)
                .orderNum(orderNum)
                .parentMenu(parent)
                .build());
    }

    private void grant(String permissionCode, Menu menu) {
        menuPermissionRepository.save(MenuPermission.builder().permissionCode(permissionCode).menu(menu).build());
    }

    // children 컬렉션을 DB 에서 다시 읽도록 영속성 컨텍스트를 비운다.
    private void reload() {
        entityManager.flush();
        entityManager.clear();
    }

    private AuthenticatedUser user(String... roles) {
        return AuthenticatedUser.builder()
                .userId(1L)
                .grantedPermissions(Arrays.stream(roles).map(role -> GrantedPermission.builder().role(role).build()).toList())
                .build();
    }

    private static List<String> names(List<MenuDto> menus) {
        return menus.stream().map(MenuDto::getMenuNm).toList();
    }

    @Test
    public void 같은_메뉴코드가_여러_애플리케이션에_있어도_요청한_애플리케이션의_트리만_준다() {
        Menu ehrRoot = menu("ehr", "ROOT", 1, null);
        menu("ehr", "GOAL", 2, ehrRoot);
        menu("ehr", "EVAL", 1, ehrRoot);

        Menu erpRoot = menu("erp", "ROOT", 1, null);
        menu("erp", "ORDER", 1, erpRoot);
        reload();

        assertEquals(List.of("ehr-EVAL", "ehr-GOAL"), names(menuService.getMenuTree("ehr", "ROOT")));
        assertEquals(List.of("erp-ORDER"), names(menuService.getMenuTree("erp", "ROOT")));
    }

    @Test
    public void 애플리케이션에_없는_메뉴코드면_빈_목록() {
        Menu erpRoot = menu("erp", "ERP_ONLY", 1, null);
        menu("erp", "ORDER", 1, erpRoot);
        reload();

        assertTrue(menuService.getMenuTree("ehr", "ERP_ONLY").isEmpty());
        assertTrue(menuService.getMenuTree("ehr", "NOTHING").isEmpty());
    }

    @Test
    public void 하위_메뉴에_다른_애플리케이션이_섞여_있으면_끝까지_걸러낸다() {
        Menu root = menu("ehr", "ROOT", 1, null);
        Menu goal = menu("ehr", "GOAL", 1, root);
        // 잘못 저장된 데이터: ehr 메뉴 아래에 erp 메뉴가 매달려 있다.
        menu("erp", "LEAK", 1, goal);
        Menu eval = menu("ehr", "EVAL", 2, root);
        menu("ehr", "EVAL_DETAIL", 1, eval);
        reload();

        List<MenuDto> tree = menuService.getMenuTree("ehr", "ROOT");

        MenuDto goalDto = tree.stream().filter(m -> m.getMenuCd().equals("GOAL")).findFirst().orElseThrow();
        assertTrue(goalDto.getChildren().isEmpty());
        // 남은 하위 메뉴가 없으면 말단으로 본다.
        assertTrue(goalDto.getIsLeafNode());

        MenuDto evalDto = tree.stream().filter(m -> m.getMenuCd().equals("EVAL")).findFirst().orElseThrow();
        assertEquals(List.of("ehr-EVAL_DETAIL"), names(evalDto.getChildren()));
        assertFalse(evalDto.getIsLeafNode());
    }

    @Test
    public void 루트_메뉴는_권한이_걸린_메뉴_중_그_애플리케이션_것만_준다() {
        Menu ehrRoot = menu("ehr", "ROOT", 1, null);
        menu("ehr", "GOAL", 1, ehrRoot);
        Menu ehrAdmin = menu("ehr", "ADMIN", 2, null);
        Menu erpRoot = menu("erp", "ROOT", 1, null);

        grant("USER", ehrRoot);
        grant("USER", erpRoot);
        grant("ADMIN", ehrAdmin);
        // 권한 두 개가 같은 메뉴를 가리키는 경우
        grant("ADMIN", ehrRoot);
        reload();

        assertEquals(List.of("ehr-ROOT"), names(menuService.getMenuRoot("ehr", user("USER"))));
        assertEquals(List.of("erp-ROOT"), names(menuService.getMenuRoot("erp", user("USER"))));

        List<MenuDto> both = menuService.getMenuRoot("ehr", user("USER", "ADMIN"));
        assertEquals(List.of("ehr-ROOT", "ehr-ADMIN"), names(both));
        assertEquals(List.of("ehr-GOAL"), names(both.get(0).getChildren()));

        assertTrue(menuService.getMenuRoot("hr", user("USER", "ADMIN")).isEmpty());
        assertTrue(menuService.getMenuRoot("ehr", user()).isEmpty());
    }
}
