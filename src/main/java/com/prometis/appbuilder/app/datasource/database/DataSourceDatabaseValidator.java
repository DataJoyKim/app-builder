package com.prometis.appbuilder.app.datasource.database;

import com.prometis.appbuilder.app.datasource.ConnectValidation;
import com.prometis.appbuilder.app.datasource.LookupKey;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

@Component
public class DataSourceDatabaseValidator {
    public ConnectValidation validateConnect(DataSourceDatabaseMeta metadata, Map<LookupKey, DataSource> dataSourceMap) {
        Connection conn = null;
        Statement stmt = null;

        LookupKey lookupKey = LookupKey.generateKey(metadata.getDataSourceName());

        DataSource dataSource = dataSourceMap.get(lookupKey);

        ConnectValidation validate = new ConnectValidation();
        try {
            Database database = DatabaseFactory.instance(metadata.getDatabaseKind());

            conn = dataSource.getConnection();
            String sql = database.getValidationQuery();
            stmt = conn.createStatement();
            stmt.executeQuery(sql);

            validate.setResult(true);
        }
        catch (Exception e) {
            validate.setResult(false);
            validate.setErrorStack(e);
        }
        finally {
            if (conn != null) {
                try {
                    conn.close();
                    stmt.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return validate;
    }
}
