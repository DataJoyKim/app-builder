package com.prometis.appbuilder.app.view.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String menuCd;
    @Column(nullable = false, length = 200)
    private String menuNm;
    @Column
    private Integer orderNum;
    @Column(length = 100)
    private String icon;
    @ManyToOne
    @JoinColumn(name = "OBJECT_ID")
    private ViewObject viewObject;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_MENU_ID")
    private Menu parentMenu;
    @OneToMany(mappedBy = "parentMenu", cascade = CascadeType.ALL)
    private List<Menu> children = new ArrayList<>();

    public void processing() {
        if(this.icon == null) {
            this.icon = (isLeafNode()) ? "far fa-circle" : "fa fa-folder";
        }

        for(Menu child : children) {
            child.processing();
        }
    }

    public void update(
            String menuCd,
            String menuNm,
            Integer orderNum,
            String icon,
            Menu parentMenu,
            ViewObject viewObject
    ) {
        this.menuCd = menuCd;
        this.menuNm = menuNm;
        this.orderNum = orderNum;
        this.parentMenu = parentMenu;
        this.viewObject = viewObject;
        this.icon = icon;
    }

    public boolean isLeafNode() {
        return children.isEmpty();
    }
}
