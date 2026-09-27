package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.Menu;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class MenuPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String permissionCode;

    @ManyToOne
    @JoinColumn(name = "MENU_ID")
    private Menu menu;

    public void update(String permissionCode, Menu menu) {
        this.permissionCode = permissionCode;
        this.menu = menu;
    }
}
