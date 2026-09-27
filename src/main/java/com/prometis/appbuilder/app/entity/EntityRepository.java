package com.prometis.appbuilder.app.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntityRepository extends JpaRepository<Entity, Long> {
    Optional<Entity> findByApplicationIdAndEntityName(String applicationId, String entityName);

    List<Entity> findByApplicationId(String applicationId);
}
