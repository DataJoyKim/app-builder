package com.prometis.appbuilder.app.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QueryRepository extends JpaRepository<Query, Long> {
    Optional<Query> findByApplicationIdAndQueryName(String applicationId, String queryName);

    List<Query> findByApplicationId(String applicationId);
}
