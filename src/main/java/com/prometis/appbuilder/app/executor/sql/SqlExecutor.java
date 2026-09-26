package com.prometis.appbuilder.app.executor.sql;

import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseRegister;
import com.prometis.appbuilder.app.datasource.LookupKey;
import com.prometis.appbuilder.app.executor.sql.parameterbind.ParameterBindType;
import com.prometis.appbuilder.app.executor.sql.parameterbind.ParameterBinder;
import com.prometis.appbuilder.app.executor.sql.parameterbind.ParameterBinderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@RequiredArgsConstructor
public class SqlExecutor {
    private final DataSource dataSource;

    public List<Map<String, Object>> execute(String sqlId, SqlQuery sqlQuery, ParameterBindType paramBindingType) throws SQLException {
        List<Map<String, Object>> resultList;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        ParameterBinder parameterBinder = ParameterBinderFactory.instance(paramBindingType);

        assert parameterBinder != null;

        sqlQuery = parameterBinder.binding(sqlQuery);

        try {
            conn = dataSource.getConnection();

            stmt = conn.prepareStatement(sqlQuery.getSql());

            for(SqlParameter sqlParameter : sqlQuery.getSqlParameters()) {
                stmt.setObject(sqlParameter.getParameterIndex(), sqlParameter.getValue());
            }

            SqlExecutorPrint.print(sqlId, sqlQuery);

            rs = stmt.executeQuery();

            resultList = mapping(rs);
        }
        finally {
            if(rs != null) {
                rs.close();
            }

            if(stmt != null) {
                stmt.close();
            }

            if(conn != null) {
                conn.close();
            }
        }

        return resultList;
    }

    private List<Map<String, Object>> mapping(ResultSet rs) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();

        ResultSetMetaData rsMeta = rs.getMetaData();
        int columnCount = rsMeta.getColumnCount();

        while(rs.next()) {
            Map<String, Object> rows = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                rows.put(rsMeta.getColumnName(i), rs.getObject(i));
            }

            resultList.add(rows);
        }

        return resultList;
    }

    public static SqlExecutor createSqlExecutor(String dataSourceName) {
        LookupKey lookupKey = LookupKey.generateKey(dataSourceName);

        DataSource dataSource = DataSourceDatabaseRegister.getDataSource(lookupKey);

        return new SqlExecutor(dataSource);
    }
}
