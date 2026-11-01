package com.prometis.appbuilder.app.security.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppSessionRepository extends JpaRepository<AppSession, Long> {
    Optional<AppSession> findByApplicationIdAndUserId(String applicationId, Long userId);

    List<AppSession> findByApplicationId(String applicationId);
}
