package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.Layout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LayoutRepository extends JpaRepository<Layout, Long> {
    Optional<Layout> findByApplicationId(String applicationId);

}
