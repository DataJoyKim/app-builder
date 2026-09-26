package com.prometis.appbuilder.app.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QueryRepository extends JpaRepository<Query, Long> {
    Optional<Query> findByQueryName(String queryName);
}
