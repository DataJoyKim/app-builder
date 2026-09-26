package com.prometis.appbuilder.app.datasource.filestorage;

import com.prometis.appbuilder.app.datasource.LookupKey;
import com.prometis.appbuilder.app.executor.file.FileStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DataSourceFileStorageRegister {
    private static Map<LookupKey, FileStorage> dataSourceMap = new ConcurrentHashMap<>();

    public static void initialize(List<DataSourceFileStorage> metadataList) {
        dataSourceMap = new ConcurrentHashMap<>();

        for(DataSourceFileStorage meta : metadataList) {
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

    public static Map<LookupKey, FileStorage> getDataSourceMap() {
        return dataSourceMap;
    }

    public static FileStorage getDataSource(LookupKey lookupKey) {
        return dataSourceMap.get(lookupKey);
    }

    public static void registry(DataSourceFileStorage meta) throws FileStorageCreationException {
        FileStorage dataSource = meta.createDataSource();

        LookupKey lookupKey = LookupKey.generateKey(meta.getDataSourceName());

        dataSourceMap.put(lookupKey, dataSource);
    }
}
