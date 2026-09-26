package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.ViewObjectContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ViewObjectContentRepository extends JpaRepository<ViewObjectContent, Long> {
    Optional<ViewObjectContent> findByObjectCode(String objectCode);
}
