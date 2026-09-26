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
public class MenuAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String authorityCode;

    @ManyToOne
    @JoinColumn(name = "MENU_ID")
    private Menu menu;

    public void update(String authorityCode, Menu menu) {
        this.authorityCode = authorityCode;
        this.menu = menu;
    }
}
