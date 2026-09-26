package com.prometis.appbuilder.app.notification;

import com.prometis.appbuilder.app.executor.notification.SendResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder @AllArgsConstructor
public class NotificationResult {
    private SendResultType resultCode;
    private String message;
}
