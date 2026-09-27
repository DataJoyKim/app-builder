package com.prometis.appbuilder.app.datasource.filestorage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceFileStorageRepository extends JpaRepository<DataSourceFileStorage, Long> {
    List<DataSourceFileStorage> findByApplicationId(String applicationId);
}
