package com.prometis.appbuilder.app.datasource.notification;

import com.prometis.appbuilder.app.datasource.LookupKey;
import com.prometis.appbuilder.app.executor.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DataSourceNotificationRegister {
    private static Map<LookupKey, NotificationSender> dataSourceMap;

    public static void initialize(List<NotificationProvider> metadataList) {
        dataSourceMap = new ConcurrentHashMap<>();

        for(NotificationProvider meta : metadataList) {
            try {
                dataSourceMap.put(LookupKey.generateKey(meta.getDataSourceName()), meta.createDataSource());
                log.info("BusinessDataSource - initialized businessDataSource : [{}]", meta.getDataSourceName());
            }
            catch (Exception e) {
                log.error("BusinessDataSource - initialize failed.. businessDataSource: [{}]", meta.getDataSourceName());
                log.error("error", e);
            }
        }
    }

    public static Map<LookupKey, NotificationSender> getDataSourceMap() {
        return dataSourceMap;
    }

    public static NotificationSender getDataSource(LookupKey lookupKey) {
        return dataSourceMap.get(lookupKey);
    }

    public static void registry(NotificationProvider meta) throws NotificationCreationException {
        NotificationSender dataSource = meta.createDataSource();

        LookupKey lookupKey = LookupKey.generateKey(meta.getDataSourceName());

        dataSourceMap.put(lookupKey, dataSource);
    }
}
