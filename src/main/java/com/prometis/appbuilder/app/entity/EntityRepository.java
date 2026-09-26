package com.prometis.appbuilder.app.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntityRepository extends JpaRepository<Entity, Long> {
    Optional<Entity> findByEntityName(String entityName);
}
