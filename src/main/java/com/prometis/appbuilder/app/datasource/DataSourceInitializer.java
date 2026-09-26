package com.prometis.appbuilder.app.datasource;

import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMeta;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseRegister;
import com.prometis.appbuilder.app.datasource.filestorage.DataSourceFileStorage;
import com.prometis.appbuilder.app.datasource.filestorage.DataSourceFileStorageRegister;
import com.prometis.appbuilder.app.datasource.notification.DataSourceNotificationRegister;
import com.prometis.appbuilder.app.datasource.notification.NotificationProvider;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServer;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServerRegister;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DataSourceInitializer {
    @Autowired
    private DataSourceService dataSourceService;

    @PostConstruct
    public void loadDataSource() {
        List<DataSourceDatabaseMeta> databases = dataSourceService.getDatabaseMetadata();
        DataSourceDatabaseRegister.initialize(databases);

        List<DataSourceRestServer> restServers = dataSourceService.getDataSourceRestServer();
        DataSourceRestServerRegister.initialize(restServers);

        List<NotificationProvider> notifications = dataSourceService.getNotificationProvider();
        DataSourceNotificationRegister.initialize(notifications);

        List<DataSourceFileStorage> fileStorages = dataSourceService.getDataSourceFileStorage();
        DataSourceFileStorageRegister.initialize(fileStorages);
    }
}
