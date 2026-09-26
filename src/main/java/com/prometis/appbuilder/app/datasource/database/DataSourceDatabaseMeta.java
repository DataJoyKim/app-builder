package com.prometis.appbuilder.app.datasource.database;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.*;
import lombok.*;

import javax.sql.DataSource;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="DATA_SOURCE_DATABASE_META_UQ",columnNames={"DATA_SOURCE_NAME"})})
@Entity
public class DataSourceDatabaseMeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String dataSourceName;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String note;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 100)
    private DatabaseKind databaseKind;

    @Column(nullable = false, length = 300)
    private String url;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column
    private Integer maximumPoolSize;

    @Column
    private Integer minimumIdle;

    @Column
    private Integer connectionTimeout;

    @Column
    private Integer validationTimeout;

    public DataSource createDataSource() {
        Database database = DatabaseFactory.instance(this.getDatabaseKind());

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(database.getDriverClassName());
        dataSource.setJdbcUrl(this.getUrl());
        dataSource.setUsername(this.getUsername());
        dataSource.setPassword(this.getPassword());
        dataSource.setMaximumPoolSize(this.getMaximumPoolSize());
        dataSource.setMinimumIdle(this.getMinimumIdle());
        dataSource.setConnectionTimeout(this.getConnectionTimeout());
        dataSource.setValidationTimeout(this.getValidationTimeout());
        dataSource.setConnectionTestQuery(database.getValidationQuery());

        return dataSource;
    }
}
