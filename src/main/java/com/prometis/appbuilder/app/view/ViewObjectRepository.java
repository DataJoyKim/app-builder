package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.ViewObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ViewObjectRepository extends JpaRepository<ViewObject, Long> {
    Optional<ViewObject> findByApplicationIdAndObjectCode(String applicationId, String objectCode);

    List<ViewObject> findByApplicationId(String applicationId);
}
