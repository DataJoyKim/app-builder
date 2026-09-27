package com.prometis.appbuilder.app.restclient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestClientRepository extends JpaRepository<RestClient, Long> {
    Optional<RestClient> findByApplicationIdAndClientName(String applicationId, String clientName);

    List<RestClient> findByApplicationId(String applicationId);
}
