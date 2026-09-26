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
        List<MenuDto> result = new ArrayList<>();
        for(Menu menu : menuList) {
            result.add(of(menu));
        }

        return result;
    }

    public static MenuDto of(Menu menu) {
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

        List<MenuDto> children = new ArrayList<>();
        if(menu.getChildren() != null) {
            for(Menu childrenMenu : menu.getChildren()) {
                children.add(of(childrenMenu));
            }
        }

        return MenuDto.builder()
                .id(menu.getId())
                .menuCd(menu.getMenuCd())
                .menuNm(menu.getMenuNm())
                .orderNum(menu.getOrderNum())
                .objectCode(objectCode)
                .objectPath(objectPath)
                .icon(menu.getIcon())
                .isLeafNode(menu.isLeafNode())
                .parentMenuCd(parentMenuCd)
                .children(children)
                .build();
    }
}
