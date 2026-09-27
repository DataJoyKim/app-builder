package com.prometis.appbuilder.console.app.repository;

import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMeta;
import com.prometis.appbuilder.app.datasource.database.DatabaseKind;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;

public interface ConsoleDatabaseMetaRepository extends JpaRepository<DataSourceDatabaseMeta, Long> {

    List<DataSourceDatabaseMeta> findByApplicationId(String applicationId);

    Optional<DataSourceDatabaseMeta> findByApplicationIdAndDataSourceName(String applicationId, String dataSourceName);

    @Transactional
    @Modifying
    @Query(" update DataSourceDatabaseMeta a " +
            " set " +
                "a.dataSourceName = :dataSourceName,"+
                "a.displayName = :displayName,"+
                "a.note = :note,"+
                "a.url = :url,"+
                "a.username = :username,"+
                "a.password = :password,"+
                "a.databaseKind = :databaseKind,"+
                "a.maximumPoolSize = :maximumPoolSize,"+
                "a.minimumIdle = :minimumIdle,"+
                "a.connectionTimeout = :connectionTimeout,"+
                "a.validationTimeout = :validationTimeout"+
            " where a.id = :id")
    void update(
            @Param("id") Long id,
            @Param("dataSourceName") String dataSourceName,
            @Param("displayName") String displayName,
            @Param("note") String note,
            @Param("url") String url,
            @Param("username") String username,
            @Param("password") String password,
            @Param("databaseKind") DatabaseKind databaseKind,
            @Param("maximumPoolSize") Integer maximumPoolSize,
            @Param("minimumIdle") Integer minimumIdle,
            @Param("connectionTimeout") Integer connectionTimeout,
            @Param("validationTimeout") Integer validationTimeout
    );

    @Transactional
    @Modifying
    @Query(" insert DataSourceDatabaseMeta a (" +
                "applicationId," +
                "dataSourceName," +
                "displayName," +
                "note," +
                "url," +
                "username," +
                "password," +
                "databaseKind," +
                "maximumPoolSize," +
                "minimumIdle," +
                "connectionTimeout," +
                "validationTimeout" +
            ") values ( " +
                ":applicationId," +
                ":dataSourceName," +
                ":displayName," +
                ":note," +
                ":url," +
                ":username," +
                ":password," +
                ":databaseKind," +
                ":maximumPoolSize," +
                ":minimumIdle," +
                ":connectionTimeout," +
                ":validationTimeout" +
            ")")
    void insert(
            @Param("applicationId") String applicationId,
            @Param("dataSourceName") String dataSourceName,
            @Param("displayName") String displayName,
            @Param("note") String note,
            @Param("url") String url,
            @Param("username") String username,
            @Param("password") String password,
            @Param("databaseKind") DatabaseKind databaseKind,
            @Param("maximumPoolSize") Integer maximumPoolSize,
            @Param("minimumIdle") Integer minimumIdle,
            @Param("connectionTimeout") Integer connectionTimeout,
            @Param("validationTimeout") Integer validationTimeout
    );
}
