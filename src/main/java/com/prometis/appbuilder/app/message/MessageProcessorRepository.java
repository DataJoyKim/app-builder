package com.prometis.appbuilder.app.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageProcessorRepository extends JpaRepository<MessageProcessor, Long> {
    Optional<MessageProcessor> findByProcessorName(String processorName);
}
