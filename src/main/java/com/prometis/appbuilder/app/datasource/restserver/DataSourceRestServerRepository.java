package com.prometis.appbuilder.app.datasource.restserver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceRestServerRepository extends JpaRepository<DataSourceRestServer, Long> {
    List<DataSourceRestServer> findByApplicationId(String applicationId);
}
