package com.prometis.appbuilder.console.app.menu;

import com.prometis.appbuilder.app.view.MenuPermissionRepository;
import com.prometis.appbuilder.app.view.MenuRepository;
import com.prometis.appbuilder.app.view.ViewObjectRepository;
import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 콘솔 Menu 화면의 트리 저장. 화면에서 추가(C)/수정(U)/삭제(D)한 메뉴들을 한 번에, 한 트랜잭션으로 저장한다 (하나라도 틀리면 아무것도 저장하지 않는다).
 *
 * 화면은 트리의 모든 메뉴를 위에서부터(상위 메뉴가 하위 메뉴보다 먼저) 보내고, 상위 메뉴는 화면의 노드 키(parentKey)로 가리킨다.
 * 그래서 아직 저장되지 않은 새 메뉴 아래에 만든 새 메뉴도 함께 저장할 수 있다.
 * 순서(orderNum)와 상위 메뉴는 트리에 보이는 그대로 모든 메뉴에 다시 맞춘다 (끌어서 옮긴 메뉴의 형제 순서까지).
 * 삭제한 메뉴의 하위 메뉴는 같이 지워지므로(Menu.children cascade) 화면이 보내지 않는다.
 */
@Service
@RequiredArgsConstructor
public class MenuTreeSaveService {
    private final MenuRepository menuRepository;
    private final ViewObjectRepository viewObjectRepository;
    private final MenuPermissionRepository menuPermissionRepository;

    /**
     * @param key       화면 트리의 노드 키
     * @param id        저장된 메뉴의 id (새 메뉴는 null)
     * @param parentKey 상위 메뉴의 노드 키 (최상위면 null)
     * @param status    C / U / D / "" (변경 없음: 순서와 상위 메뉴만 맞춘다)
     */
    public record MenuItem(String key, Long id, String parentKey, String status,
                           String menuCd, String menuNm, String objectCode, String icon, Integer orderNum) {
        boolean isCreate() { return "C".equals(status); }
        boolean isUpdate() { return "U".equals(status); }
        boolean isDelete() { return "D".equals(status); }
    }

    public record SaveResult(int created, int updated, int deleted) {
    }

    public static class MenuSaveException extends RuntimeException {
        public MenuSaveException(String message) {
            super(message);
        }
    }

    @Transactional
    public SaveResult save(String applicationId, List<MenuItem> items) {
        Map<Long, Menu> saved = new HashMap<>();
        for(Menu menu : menuRepository.findByApplicationId(applicationId)) {
            saved.put(menu.getId(), menu);
        }

        validate(items, saved);

        // 1) 삭제: 하위 메뉴까지 cascade 로 지워지므로, 그 메뉴들의 메뉴접근권한을 먼저 지운다
        int deleted = 0;
        for(MenuItem item : items) {
            if(!item.isDelete()) continue;
            Menu menu = saved.get(item.id());
            if(menu == null) continue;

            for(Menu target : withDescendants(menu)) {
                menuPermissionRepository.deleteAll(menuPermissionRepository.findByMenu(target));
                saved.remove(target.getId());
            }
            if(menu.getParentMenu() != null) {
                menu.getParentMenu().getChildren().remove(menu);
            }
            menuRepository.delete(menu);
            deleted++;
        }
        menuRepository.flush();

        // 2) 추가/수정/순서: 위에서부터 차례로 저장해 하위 메뉴가 상위 메뉴(새 메뉴 포함)를 찾을 수 있게 한다
        Map<String, Menu> byKey = new HashMap<>();
        int created = 0;
        int updated = 0;
        for(MenuItem item : items) {
            if(item.isDelete()) continue;

            Menu parent = item.parentKey() == null ? null : byKey.get(item.parentKey());
            if(item.parentKey() != null && parent == null) {
                throw new MenuSaveException("[" + item.menuNm() + "] 의 상위 메뉴를 찾을 수 없습니다. 화면을 새로고침한 뒤 다시 저장해주세요.");
            }
            int orderNum = item.orderNum() == null ? 0 : item.orderNum();

            Menu menu;
            if(item.isCreate()) {
                menu = menuRepository.save(Menu.builder()
                        .applicationId(applicationId)
                        .menuCd(item.menuCd().trim())
                        .menuNm(item.menuNm().trim())
                        .orderNum(orderNum)
                        .icon(blankToNull(item.icon()))
                        .parentMenu(parent)
                        .viewObject(viewObjectOf(applicationId, item.objectCode()))
                        .children(new ArrayList<>())
                        .build());
                created++;
            }
            else {
                menu = saved.get(item.id());
                if(item.isUpdate()) {
                    menu.update(item.menuCd().trim(), item.menuNm().trim(), orderNum, blankToNull(item.icon()),
                            parent, viewObjectOf(applicationId, item.objectCode()));
                    updated++;
                }
                else if(!Objects.equals(menu.getOrderNum(), orderNum) || !Objects.equals(idOf(menu.getParentMenu()), idOf(parent))) {
                    // 내용은 그대로이고 트리에서의 위치만 맞춘다
                    menu.update(menu.getMenuCd(), menu.getMenuNm(), orderNum, menu.getIcon(), parent, menu.getViewObject());
                }
            }

            byKey.put(item.key(), menu);
        }

        return new SaveResult(created, updated, deleted);
    }

    private void validate(List<MenuItem> items, Map<Long, Menu> saved) {
        Set<String> keys = new HashSet<>();
        Map<String, String> menuCds = new HashMap<>();   // 저장 뒤 남을 메뉴코드 → 메뉴명

        for(MenuItem item : items) {
            if(item.key() == null || !keys.add(item.key())) {
                throw new MenuSaveException("메뉴 정보가 올바르지 않습니다. 화면을 새로고침한 뒤 다시 저장해주세요.");
            }
            if(!item.isCreate() && (item.id() == null || !saved.containsKey(item.id()))) {
                throw new MenuSaveException("[" + item.menuNm() + "] 메뉴가 이미 삭제되었거나 다른 애플리케이션의 메뉴입니다. 화면을 새로고침해주세요.");
            }
            if(item.isDelete()) continue;

            String menuCd;
            String menuNm;
            if(item.isCreate() || item.isUpdate()) {
                menuCd = item.menuCd() == null ? "" : item.menuCd().trim();
                menuNm = item.menuNm() == null ? "" : item.menuNm().trim();
                if(menuCd.isEmpty() || menuNm.isEmpty()) {
                    throw new MenuSaveException("메뉴코드와 메뉴명을 입력해주세요. (" + (menuNm.isEmpty() ? menuCd : menuNm) + ")");
                }
                if(menuCd.length() > 100 || menuNm.length() > 200) {
                    throw new MenuSaveException("[" + menuNm + "] 메뉴코드는 100자, 메뉴명은 200자를 넘을 수 없습니다.");
                }
            }
            else {
                Menu menu = saved.get(item.id());
                menuCd = menu.getMenuCd();
                menuNm = menu.getMenuNm();
            }

            String other = menuCds.put(menuCd, menuNm);
            if(other != null) {
                throw new MenuSaveException("메뉴코드 [" + menuCd + "] 가 [" + other + "], [" + menuNm + "] 에 중복되어 있습니다.");
            }
        }
    }

    private ViewObject viewObjectOf(String applicationId, String objectCode) {
        if(objectCode == null || objectCode.isBlank()) {
            return null;
        }
        return viewObjectRepository.findByApplicationIdAndObjectCode(applicationId, objectCode.trim())
                .orElseThrow(() -> new MenuSaveException("화면 [" + objectCode + "] 을 찾을 수 없습니다."));
    }

    private static List<Menu> withDescendants(Menu menu) {
        List<Menu> result = new ArrayList<>();
        Deque<Menu> stack = new ArrayDeque<>(List.of(menu));
        while(!stack.isEmpty()) {
            Menu current = stack.pop();
            result.add(current);
            stack.addAll(current.getChildren());
        }
        return result;
    }

    // 상위 메뉴는 LAZY 프록시일 수 있어 객체가 아니라 id 로 비교한다 (프록시의 getId 는 초기화하지 않는다)
    private static Long idOf(Menu menu) {
        return menu == null ? null : menu.getId();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
