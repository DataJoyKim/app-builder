package com.prometis.appbuilder.app.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageTypeRepository extends JpaRepository<StorageType, Long> {
    List<StorageType> findByApplicationIdOrderByStorageTypeAsc(String applicationId);

    Optional<StorageType> findByApplicationIdAndStorageType(String applicationId, String storageType);
}
