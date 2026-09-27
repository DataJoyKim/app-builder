package com.prometis.appbuilder.app.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileHandlerRepository extends JpaRepository<FileHandler, Long> {
    Optional<FileHandler> findByApplicationIdAndHandlerName(String applicationId, String handlerName);

    List<FileHandler> findByApplicationId(String applicationId);
}
