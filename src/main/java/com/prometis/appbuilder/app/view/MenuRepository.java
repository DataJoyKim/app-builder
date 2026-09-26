package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu,Long> {
    @Query("select m from Menu m left join fetch m.children where m.parentMenu is null ORDER BY m.orderNum ASC")
    List<Menu> findAllTree();

    Optional<Menu> findByMenuCd(String menuCd);

    Optional<Menu> findByParentMenu(Menu parentMenu);
    @Query("select m from Menu m left join fetch m.children where m.parentMenu = :parentMenu ORDER BY m.orderNum ASC")
    List<Menu> findAllTree(@Param("parentMenu") Menu parentMenu);

    @Query("select m from Menu m left join fetch m.parentMenu where m.viewObject = :viewObject")
    List<Menu> findByViewObject(@Param("viewObject") ViewObject viewObject);
}
