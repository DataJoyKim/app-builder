package com.prometis.appbuilder.console.repository;

import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServer;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsoleRestServerRepository extends JpaRepository<DataSourceRestServer, Long> {

    @Transactional
    @Modifying
    @Query(" update DataSourceRestServer a " +
            " set " +
                "a.dataSourceName = :dataSourceName,"+
                "a.displayName = :displayName,"+
                "a.note = :note,"+
                "a.baseUrl = :baseUrl,"+
                "a.connectTimeout = :connectTimeout,"+
                "a.connectRequestTimeout = :connectRequestTimeout,"+
                "a.connectionMaxTotal = :connectionMaxTotal,"+
                "a.connectionDefaultMaxPerRoute = :connectionDefaultMaxPerRoute"+
            " where a.id = :id")
    void update(
            @Param("id") Long id,
            @Param("dataSourceName") String dataSourceName,
            @Param("displayName") String displayName,
            @Param("note") String note,
            @Param("baseUrl") String baseUrl,
            @Param("connectTimeout") Integer connectTimeout,
            @Param("connectRequestTimeout") Integer connectRequestTimeout,
            @Param("connectionMaxTotal") Integer connectionMaxTotal,
            @Param("connectionDefaultMaxPerRoute") Integer connectionDefaultMaxPerRoute
    );

    @Transactional
    @Modifying
    @Query(" insert DataSourceRestServer a (" +
                "dataSourceName," +
                "displayName," +
                "note," +
                "baseUrl,"+
                "connectTimeout,"+
                "connectRequestTimeout,"+
                "connectionMaxTotal,"+
                "connectionDefaultMaxPerRoute"+
            ") values ( " +
                ":dataSourceName," +
                ":displayName," +
                ":note," +
                ":baseUrl,"+
                ":connectTimeout,"+
                ":connectRequestTimeout,"+
                ":connectionMaxTotal,"+
                ":connectionDefaultMaxPerRoute"+
            ")")
    void insert(
            @Param("dataSourceName") String dataSourceName,
            @Param("displayName") String displayName,
            @Param("note") String note,
            @Param("baseUrl") String baseUrl,
            @Param("connectTimeout") Integer connectTimeout,
            @Param("connectRequestTimeout") Integer connectRequestTimeout,
            @Param("connectionMaxTotal") Integer connectionMaxTotal,
            @Param("connectionDefaultMaxPerRoute") Integer connectionDefaultMaxPerRoute
    );
}
