package com.prometis.appbuilder.platform.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByApplicationId(String applicationId);

    List<Application> findByApplicationIdIn(Collection<String> applicationIds);
}
