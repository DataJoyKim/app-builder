package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuAuthorityRepository extends JpaRepository<MenuAuthority, Long> {
    List<MenuAuthority> findByMenu(Menu menu);

    List<MenuAuthority> findByAuthorityCode(String authorityCode);
}
