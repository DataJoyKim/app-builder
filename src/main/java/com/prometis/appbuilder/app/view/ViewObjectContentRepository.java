package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.ViewObjectContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ViewObjectContentRepository extends JpaRepository<ViewObjectContent, Long> {
    Optional<ViewObjectContent> findByApplicationIdAndObjectCode(String applicationId, String objectCode);

    List<ViewObjectContent> findByApplicationId(String applicationId);
}
