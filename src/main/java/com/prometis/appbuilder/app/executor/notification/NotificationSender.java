package com.prometis.appbuilder.app.executor.notification;

public interface NotificationSender {
    SendResult send(String sendId, String to, String subject, String content);

    SendResult validate();
}
