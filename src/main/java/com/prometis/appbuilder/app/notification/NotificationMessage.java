package com.prometis.appbuilder.app.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @AllArgsConstructor @Builder
public class NotificationMessage {
    private String to;
    private String subject;
    private String content;
}
