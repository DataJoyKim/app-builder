package com.prometis.appbuilder.app.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByApplicationIdAndNotificationName(String applicationId, String notificationName);

    List<Notification> findByApplicationId(String applicationId);
}

