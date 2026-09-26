package com.prometis.appbuilder.app.datasource.database;

import com.prometis.appbuilder.app.datasource.LookupKey;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DataSourceDatabaseRegister {
    private static Map<LookupKey, DataSource> dataSourceMap;

    public static void initialize(List<DataSourceDatabaseMeta> metadataList) {
        dataSourceMap = new ConcurrentHashMap<>();

        for(DataSourceDatabaseMeta meta : metadataList) {
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

    public static Map<LookupKey, DataSource> getDataSourceMap() {
        return dataSourceMap;
    }

    public static DataSource getDataSource(LookupKey lookupKey) {
        return dataSourceMap.get(lookupKey);
    }

    public static void registry(DataSourceDatabaseMeta meta) {
        DataSource dataSource = meta.createDataSource();

        LookupKey lookupKey = LookupKey.generateKey(meta.getDataSourceName());

        dataSourceMap.put(lookupKey, dataSource);
    }
}
