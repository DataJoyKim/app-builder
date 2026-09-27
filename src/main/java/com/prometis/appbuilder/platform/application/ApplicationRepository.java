package com.prometis.appbuilder.platform.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByApplicationId(String applicationId);
}
