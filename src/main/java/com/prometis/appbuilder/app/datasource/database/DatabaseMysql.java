package com.prometis.appbuilder.app.datasource.database;

public class DatabaseMysql implements Database {
    @Override
    public String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String getValidationQuery() {
        return "select 1";
    }
}
