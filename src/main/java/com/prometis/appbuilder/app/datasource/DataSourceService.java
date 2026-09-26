package com.prometis.appbuilder.app.datasource;

import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMeta;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMetaRepository;
import com.prometis.appbuilder.app.datasource.filestorage.DataSourceFileStorage;
import com.prometis.appbuilder.app.datasource.filestorage.DataSourceFileStorageRepository;
import com.prometis.appbuilder.app.datasource.notification.NotificationProvider;
import com.prometis.appbuilder.app.datasource.notification.NotificationProviderRepository;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServer;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {
    private final DataSourceDatabaseMetaRepository dataSourceDatabaseMetaRepository;
    private final DataSourceRestServerRepository dataSourceRestServerRepository;
    private final NotificationProviderRepository notificationProviderRepository;
    private final DataSourceFileStorageRepository dataSourceFileStorageRepository;

    public List<DataSourceDatabaseMeta> getDatabaseMetadata() {
        return dataSourceDatabaseMetaRepository.findAll();
    }

    public List<DataSourceRestServer> getDataSourceRestServer() {
        return dataSourceRestServerRepository.findAll();
    }

    public List<NotificationProvider> getNotificationProvider() {
        return notificationProviderRepository.findAll();
    }

    public List<DataSourceFileStorage> getDataSourceFileStorage() {
        return dataSourceFileStorageRepository.findAll();
    }
}
