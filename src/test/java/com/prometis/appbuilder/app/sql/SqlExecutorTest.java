package com.prometis.appbuilder.app.sql;

import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMeta;
import com.prometis.appbuilder.app.datasource.database.DatabaseKind;
import com.prometis.appbuilder.app.executor.sql.SqlExecutor;
import com.prometis.appbuilder.app.executor.sql.SqlParameter;
import com.prometis.appbuilder.app.executor.sql.SqlQuery;
import com.prometis.appbuilder.app.executor.sql.parameterbind.ParameterBindType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SqlExecutorTest {

    @Test
    public void executeTest() throws SQLException {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put("dataSourceName","dataSourceGoal");
        params.put("displayName", "test");
        params.put("note", "test");
        params.put("url", "jdbc:mariadb://localhost:3306/goalset");
        params.put("username", "D1ADMIN");
        params.put("password", "rlaskrdud1!");
        params.put("databaseKind","MARIADB");
        params.put("maximumPoolSize", "10");
        params.put("minimumIdle", "10");
        params.put("connectionTimeout", "60000");
        params.put("validationTimeout", "60000");

        String createSql = """
                CREATE TABLE `test` (
                    `test` VARCHAR(10) NOT NULL COLLATE 'utf8_general_ci'
                )
                COLLATE='utf8_general_ci'
                ENGINE=InnoDB
                AUTO_INCREMENT=14;
                """;

        String insertSql = "insert test(test) values(#{test});";

        List<SqlParameter> insertParams = new ArrayList<>();
        insertParams.add(SqlParameter.builder()
                .parameterName("test")
                .value("init")
                .parameterIndex(1)
                .build());

        String selectSql = "select * from test where test = #{test} order by test;";

        List<SqlParameter> selectParams1 = new ArrayList<>();
        selectParams1.add(SqlParameter.builder()
                .parameterName("test")
                .value("init")
                .parameterIndex(1)
                .build());

        List<SqlParameter> selectParams2 = new ArrayList<>();
        selectParams2.add(SqlParameter.builder()
                .parameterName("test")
                .value("success")
                .parameterIndex(1)
                .build());

        String updateSql = "update test set test = #{test} where test = #{whereTest};";

        List<SqlParameter> updateParams = new ArrayList<>();
        updateParams.add(SqlParameter.builder()
                .parameterName("test")
                .value("success")
                .parameterIndex(1)
                .build());
        updateParams.add(SqlParameter.builder()
                .parameterName("whereTest")
                .value("init")
                .parameterIndex(2)
                .build());

        String deleteSql = "delete from test where test = #{test};";

        List<SqlParameter> deleteParams = new ArrayList<>();
        deleteParams.add(SqlParameter.builder()
                .parameterName("test")
                .value("success")
                .parameterIndex(1)
                .build());

        String dropSql = "DROP TABLE `test`;";

        // When
        DataSourceDatabaseMeta meta = DataSourceDatabaseMeta.builder()
                .dataSourceName((String) params.get("dataSourceName"))
                .displayName((String) params.get("displayName"))
                .note((String) params.get("note"))
                .url((String) params.get("url"))
                .username((String) params.get("username"))
                .password((String) params.get("password"))
                .databaseKind(DatabaseKind.valueOf((String) params.get("databaseKind")))
                .maximumPoolSize(Integer.valueOf((String) params.get("maximumPoolSize")))
                .minimumIdle(Integer.valueOf((String) params.get("minimumIdle")))
                .connectionTimeout(Integer.valueOf((String) params.get("connectionTimeout")))
                .validationTimeout(Integer.valueOf((String) params.get("validationTimeout")))
                .build();

        DataSource dataSource = meta.createDataSource();

        SqlExecutor executor = new SqlExecutor(dataSource);

        executor.execute("TEST", SqlQuery.builder()
                .sql(createSql)
                .build(), ParameterBindType.NAME_BIND);

        executor.execute("TEST", SqlQuery.builder()
                .sql(insertSql)
                .sqlParameters(insertParams)
                .build(), ParameterBindType.NAME_BIND);

        List<Map<String,Object>> selectResults = executor.execute("TEST", SqlQuery.builder()
                .sql(selectSql)
                .sqlParameters(selectParams1)
                .build(), ParameterBindType.NAME_BIND);

        executor.execute("TEST", SqlQuery.builder()
                .sql(updateSql)
                .sqlParameters(updateParams)
                .build(), ParameterBindType.NAME_BIND);

        List<Map<String,Object>> selectResults2 = executor.execute("TEST", SqlQuery.builder()
                .sql(selectSql)
                .sqlParameters(selectParams2)
                .build(), ParameterBindType.NAME_BIND);

        executor.execute("TEST", SqlQuery.builder()
                .sql(deleteSql)
                .sqlParameters(deleteParams)
                .build(), ParameterBindType.NAME_BIND);

        List<Map<String,Object>> selectResults3 = executor.execute("TEST", SqlQuery.builder()
                .sql(selectSql)
                .sqlParameters(selectParams2)
                .build(), ParameterBindType.NAME_BIND);

        executor.execute("TEST", SqlQuery.builder()
                .sql(dropSql)
                .build(), ParameterBindType.NAME_BIND);

        // Then
        Assertions.assertEquals(1, selectResults.size());
        Assertions.assertEquals(1, selectResults2.size());
        Assertions.assertEquals(0, selectResults3.size());
    }
}