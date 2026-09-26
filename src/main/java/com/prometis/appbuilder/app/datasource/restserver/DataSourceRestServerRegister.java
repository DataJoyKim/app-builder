package com.prometis.appbuilder.app.datasource.restserver;

import com.prometis.appbuilder.app.datasource.LookupKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DataSourceRestServerRegister {
    private static Map<LookupKey, RestClient> dataSourceMap;

    public static void initialize(List<DataSourceRestServer> metadataList) {
        dataSourceMap = new ConcurrentHashMap<>();

        for(DataSourceRestServer meta : metadataList) {
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

    public static Map<LookupKey, RestClient> getDataSourceMap() {
        return dataSourceMap;
    }

    public static RestClient getDataSource(LookupKey lookupKey) {
        return dataSourceMap.get(lookupKey);
    }

    public static void registry(DataSourceRestServer meta) {
        RestClient dataSource = meta.createDataSource();

        LookupKey lookupKey = LookupKey.generateKey(meta.getDataSourceName());

        dataSourceMap.put(lookupKey, dataSource);
    }
}
