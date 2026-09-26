package com.prometis.appbuilder.app.datasource.database;

public class DatabaseMssql implements Database {
    @Override
    public String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getValidationQuery() {
        return "select 1";
    }
}
