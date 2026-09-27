package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu,Long> {
    @Query("select m from Menu m left join fetch m.children where m.applicationId = :applicationId and m.parentMenu is null ORDER BY m.orderNum ASC")
    List<Menu> findAllTree(@Param("applicationId") String applicationId);

    // menuCd 는 애플리케이션 안에서만 유일하다(MENU_UQ). 런타임 조회는 이 메소드를 쓴다.
    Optional<Menu> findByApplicationIdAndMenuCd(String applicationId, String menuCd);

    Optional<Menu> findByParentMenu(Menu parentMenu);

    @Query("select m from Menu m left join fetch m.children where m.parentMenu = :parentMenu and m.applicationId = :applicationId ORDER BY m.orderNum ASC")
    List<Menu> findAllTree(@Param("applicationId") String applicationId, @Param("parentMenu") Menu parentMenu);

    @Query("select m from Menu m left join fetch m.parentMenu where m.viewObject = :viewObject")
    List<Menu> findByViewObject(@Param("viewObject") ViewObject viewObject);
}
