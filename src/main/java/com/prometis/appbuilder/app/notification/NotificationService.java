package com.prometis.appbuilder.app.notification;

import com.prometis.appbuilder.app.datasource.LookupKey;
import com.prometis.appbuilder.app.datasource.notification.DataSourceNotificationRegister;
import com.prometis.appbuilder.app.executor.notification.NotificationSender;
import com.prometis.appbuilder.app.executor.notification.SendResult;
import com.prometis.appbuilder.app.executor.notification.SendResultType;
import com.prometis.appbuilder.app.expression.ParameterExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationResult execute(String notificationName, NotificationRequest params) {

        Notification notification = notificationRepository.findByNotificationName(notificationName)
                .orElseThrow();

        NotificationMessage message = notification.createMessage(new ParameterExpression(), params);

        NotificationSender notificationSender = DataSourceNotificationRegister.getDataSource(LookupKey.generateKey(notification.getDataSourceName()));

        if(notification.getEnableSend() != null && notification.getEnableSend()) {
            SendResult sendResult = notificationSender.send(notificationName, message.getTo(), message.getSubject(), message.getContent());

            return NotificationResult.builder()
                    .resultCode(sendResult.getResultType())
                    .message((sendResult.getResultType() == SendResultType.SUCCESS) ? "전송 성공하였습니다." :sendResult.getErrorMessage())
                    .build();
        }
        else {
            return NotificationResult.builder()
                    .resultCode(SendResultType.SUCCESS)
                    .message("전송 성공하였습니다. 전송 활성화가 off 되어있어 실제 발송되지않습니다.")
                    .build();
        }
    }
}
