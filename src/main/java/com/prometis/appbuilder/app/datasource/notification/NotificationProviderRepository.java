package com.prometis.appbuilder.app.datasource.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationProviderRepository extends JpaRepository<NotificationProvider, Long> {
    List<NotificationProvider> findByApplicationId(String applicationId);
}
