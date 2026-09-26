package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.ViewAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViewActionRepository extends JpaRepository<ViewAction, Long> {
    List<ViewAction> findByObjectCode(String objectCode);
}
