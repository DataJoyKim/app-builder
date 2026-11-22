package com.prometis.appbuilder.console.app.menu;

import com.prometis.appbuilder.app.view.MenuPermission;
import com.prometis.appbuilder.app.view.MenuPermissionRepository;
import com.prometis.appbuilder.app.view.MenuRepository;
import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.console.app.menu.MenuTreeSaveService.MenuItem;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 콘솔 Menu 화면의 트리 저장: 여러 메뉴의 추가/수정/삭제/순서를 한 번에 저장하는지 실제 DB(H2)로 확인한다.
 */
@DataJpaTest
@Import(MenuTreeSaveService.class)
class MenuTreeSaveServiceTest {
    private static final String APP = "ehr";

    @Autowired
    private MenuTreeSaveService service;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private MenuPermissionRepository menuPermissionRepository;
    @Autowired
    private EntityManager entityManager;

    private Menu menu(String code, Menu parent, int order) {
        Menu menu = menuRepository.save(Menu.builder().applicationId(APP).menuCd(code).menuNm(code + " 메뉴")
                .orderNum(order).parentMenu(parent).children(new ArrayList<>()).build());
        if(parent != null) parent.getChildren().add(menu);
        return menu;
    }

    private static MenuItem create(String key, String parentKey, String code, int order) {
        return new MenuItem(key, null, parentKey, "C", code, code + " 메뉴", "", null, order);
    }

    private static MenuItem keep(Menu menu, String parentKey, int order) {
        return new MenuItem("k" + menu.getId(), menu.getId(), parentKey, "", menu.getMenuCd(), menu.getMenuNm(), "", null, order);
    }

    private Map<String, Menu> reload() {
        entityManager.flush();
        entityManager.clear();
        return menuRepository.findByApplicationId(APP).stream().collect(Collectors.toMap(Menu::getMenuCd, m -> m));
    }

    @Test
    void 새_메뉴_여러개와_새_메뉴_아래의_새_메뉴를_한번에_저장한다() {
        Menu home = menu("HOME", null, 0);

        MenuTreeSaveService.SaveResult result = service.save(APP, List.of(
                keep(home, null, 0),
                create("n1", null, "HR", 1),
                create("n2", "n1", "HR_EMP", 0),       // 저장 전의 새 메뉴(HR) 아래
                create("n3", "n1", "HR_DEPT", 1),
                create("n4", null, "SALES", 2)));

        assertEquals(4, result.created());
        Map<String, Menu> menus = reload();
        assertEquals(5, menus.size());
        assertEquals("HR", menus.get("HR_EMP").getParentMenu().getMenuCd());
        assertEquals("HR", menus.get("HR_DEPT").getParentMenu().getMenuCd());
        assertNull(menus.get("SALES").getParentMenu());
        assertEquals(2, menus.get("SALES").getOrderNum());
    }

    @Test
    void 수정_삭제_이동을_한번에_반영하고_삭제한_메뉴의_하위메뉴와_권한도_지운다() {
        Menu a = menu("A", null, 0);
        Menu b = menu("B", null, 1);
        Menu b1 = menu("B1", b, 0);
        Menu c = menu("C", null, 2);
        Menu c1 = menu("C1", c, 0);
        menuPermissionRepository.save(MenuPermission.builder().permissionCode("P1").menu(b).build());
        menuPermissionRepository.save(MenuPermission.builder().permissionCode("P1").menu(b1).build());

        service.save(APP, List.of(
                new MenuItem("k" + c.getId(), c.getId(), null, "U", "C", "C 새이름", "", "fas fa-star", 0),  // 이름 바꾸고 맨 앞으로
                keep(c1, "k" + c.getId(), 0),
                keep(a, null, 1),
                new MenuItem("k" + b.getId(), b.getId(), null, "D", "B", "B 메뉴", "", null, 2)));        // B1 은 함께 삭제

        Map<String, Menu> menus = reload();
        assertEquals(List.of("A", "C", "C1"), menus.keySet().stream().sorted().toList());
        assertEquals("C 새이름", menus.get("C").getMenuNm());
        assertEquals("fas fa-star", menus.get("C").getIcon());
        assertEquals(0, menus.get("C").getOrderNum());
        assertEquals(1, menus.get("A").getOrderNum());   // 상태 변경 없이 순서만 바뀐 메뉴도 맞춘다
        assertTrue(menuPermissionRepository.findAll().isEmpty());
    }

    @Test
    void 메뉴코드가_겹치면_아무것도_저장하지_않는다() {
        Menu a = menu("A", null, 0);

        MenuTreeSaveService.MenuSaveException e = assertThrows(MenuTreeSaveService.MenuSaveException.class,
                () -> service.save(APP, List.of(
                        keep(a, null, 0),
                        create("n1", null, "NEW", 1),
                        create("n2", null, "A", 2))));
        assertTrue(e.getMessage().contains("[A]"));

        assertThrows(MenuTreeSaveService.MenuSaveException.class,
                () -> service.save(APP, List.of(create("n1", null, " ", 0))));

        assertEquals(List.of("A"), reload().keySet().stream().sorted(Comparator.naturalOrder()).toList());
    }

    @Test
    void 다른_애플리케이션의_메뉴는_건드리지_않는다() {
        Menu other = menuRepository.save(Menu.builder().applicationId("crm").menuCd("X").menuNm("X").orderNum(0).children(new ArrayList<>()).build());

        assertThrows(MenuTreeSaveService.MenuSaveException.class,
                () -> service.save(APP, List.of(new MenuItem("k1", other.getId(), null, "D", "X", "X", "", null, 0))));
        assertTrue(menuRepository.findById(other.getId()).isPresent());
    }
}
