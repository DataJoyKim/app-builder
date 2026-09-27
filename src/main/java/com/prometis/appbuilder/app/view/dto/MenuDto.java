package com.prometis.appbuilder.app.view.dto;

import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter @AllArgsConstructor @Builder
public class MenuDto {
    private Long id;
    private String menuCd;
    private String menuNm;
    private Integer orderNum;
    private String objectCode;
    private String objectPath;
    private String parentMenuCd;
    private String icon;
    private Boolean isLeafNode;
    private List<MenuDto> children;

    public static List<MenuDto> of(List<Menu> menuList) {
        return of(menuList, null);
    }

    /**
     * applicationId 가 있으면 그 애플리케이션의 메뉴만 담는다. 하위 메뉴도 끝까지 같은 기준으로 거르고,
     * 걸러진 뒤 남은 하위 메뉴가 없으면 말단(isLeafNode)으로 본다. null 이면 거르지 않는다.
     */
    public static List<MenuDto> of(List<Menu> menuList, String applicationId) {
        List<MenuDto> result = new ArrayList<>();
        for(Menu menu : menuList) {
            if(belongsTo(menu, applicationId)) {
                result.add(of(menu, applicationId));
            }
        }

        return result;
    }

    public static MenuDto of(Menu menu) {
        return of(menu, null);
    }

    private static boolean belongsTo(Menu menu, String applicationId) {
        return menu != null && (applicationId == null || applicationId.equals(menu.getApplicationId()));
    }

    private static MenuDto of(Menu menu, String applicationId) {
        if(menu == null) {
            return null;
        }

        ViewObject viewObject = menu.getViewObject();
        String objectCode = null;
        String objectPath = null;
        if(viewObject != null) {
            objectCode = viewObject.getObjectCode();
            objectPath = viewObject.getPath();
        }

        Menu parentMenu = menu.getParentMenu();
        String parentMenuCd = null;
        if(parentMenu != null) {
            parentMenuCd = parentMenu.getMenuCd();
        }

        List<MenuDto> children = menu.getChildren() == null
                ? new ArrayList<>()
                : of(menu.getChildren(), applicationId);

        return MenuDto.builder()
                .id(menu.getId())
                .menuCd(menu.getMenuCd())
                .menuNm(menu.getMenuNm())
                .orderNum(menu.getOrderNum())
                .objectCode(objectCode)
                .objectPath(objectPath)
                .icon(menu.getIcon())
                .isLeafNode(applicationId == null ? menu.isLeafNode() : children.isEmpty())
                .parentMenuCd(parentMenuCd)
                .children(children)
                .build();
    }
}
