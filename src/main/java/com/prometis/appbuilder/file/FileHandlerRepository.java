package com.prometis.appbuilder.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileHandlerRepository extends JpaRepository<FileHandler, Long> {
    Optional<FileHandler> findByHandlerName(String handlerName);
}
