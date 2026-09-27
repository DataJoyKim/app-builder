package com.prometis.appbuilder.app.datasource.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceDatabaseMetaRepository extends JpaRepository<DataSourceDatabaseMeta, Long> {
    List<DataSourceDatabaseMeta> findByApplicationId(String applicationId);
}
