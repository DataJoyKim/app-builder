package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.ViewObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ViewObjectRepository extends JpaRepository<ViewObject, Long> {
    Optional<ViewObject> findByObjectCode(String objectCode);
}
