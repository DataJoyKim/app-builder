package com.prometis.appbuilder.app.datasource.notification;

import com.prometis.appbuilder.app.datasource.ConnectValidation;
import com.prometis.appbuilder.app.datasource.LookupKey;
import com.prometis.appbuilder.app.executor.notification.NotificationSender;
import com.prometis.appbuilder.app.executor.notification.SendResult;
import com.prometis.appbuilder.app.executor.notification.SendResultType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationValidator {
    public ConnectValidation validateConnect(NotificationProvider metadata, Map<LookupKey, NotificationSender> dataSourceMap) {
        ConnectValidation validate = new ConnectValidation();

        LookupKey lookupKey = LookupKey.generateKey(metadata.getDataSourceName());

        NotificationSender notification = dataSourceMap.get(lookupKey);

        try {
            SendResult result = notification.validate();

            if(SendResultType.SUCCESS.equals(result.getResultType())) {
                validate.setResult(true);
            }
            else {
                validate.setResult(false);
                validate.setErrorStack(new RuntimeException("Notification Send Failed..."));
            }
        }
        catch (Exception e) {
            validate.setResult(false);
            validate.setErrorStack(e);
        }

        return validate;
    }
}
