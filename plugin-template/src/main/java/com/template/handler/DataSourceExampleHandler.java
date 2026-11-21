package com.template.handler;

import com.prometis.plugin.api.PluginContext;
import com.prometis.plugin.api.PluginRequest;
import com.prometis.plugin.api.PluginResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 호스트 애플리케이션의 데이터소스 사용 예시.
 *
 * <p>플러그인은 DB 접속정보를 따로 갖지 않고, 애플리케이션 콘솔 > DataSource > Database 에 등록된 데이터소스를
 * {@link PluginContext#getDataSource(String)} 로 DataSource 명으로 꺼내 쓴다. 이 플러그인을 실행한 애플리케이션의 데이터소스만 보인다.</p>
 *
 * <p>규칙</p>
 * <ul>
 *     <li>Connection / PreparedStatement / ResultSet 은 try-with-resources 로 반드시 닫는다 (커넥션 풀은 호스트가 관리한다).</li>
 *     <li>요청 값은 SQL 문자열에 이어 붙이지 말고 {@code ?} 바인딩으로 넘긴다 (SQL 인젝션 방지).</li>
 *     <li>DataSource 자체를 close 하거나 형변환해서 설정을 바꾸지 않는다 (호스트가 막아둔다).</li>
 * </ul>
 */
public class DataSourceExampleHandler {
    /** 요청메시지에 dataSourceName 이 없을 때 쓰는 DataSource 명. 콘솔에 등록한 이름으로 바꿔서 쓴다 */
    private static final String DEFAULT_DATA_SOURCE_NAME = "sample-db";

    /**
     * template/db-query 에서 실행하는 SQL. 실제 플러그인에서는 업무에 맞는 SQL 로 바꾼다.
     * 요청메시지 행의 keyword 값을 ? 에 바인딩한다.
     */
    private static final String QUERY_SQL = "SELECT * FROM sample_table WHERE name LIKE ?";
    private static final int MAX_ROWS = 1000;

    /**
     * pluginPath: template/db-ping
     * 요청메시지 첫 행: { "dataSourceName": "sample-db" } (생략하면 {@link #DEFAULT_DATA_SOURCE_NAME})
     */
    public PluginResult ping(PluginRequest request, PluginContext context) throws SQLException {
        String dataSourceName = dataSourceNameOf(request);
        DataSource dataSource = context.getDataSource(dataSourceName);

        try (Connection connection = dataSource.getConnection()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("dataSourceName", dataSourceName);
            result.put("valid", connection.isValid(3));
            result.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
            result.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
            return PluginResult.success(result);
        }
    }

    /**
     * pluginPath: template/db-query
     * 요청메시지 행: { "dataSourceName": "sample-db", "keyword": "kim" }. 행마다 한 번씩 조회하고 결과 행을 모두 모아 돌려준다.
     */
    public PluginResult query(PluginRequest request, PluginContext context) throws SQLException {
        String dataSourceName = dataSourceNameOf(request);
        DataSource dataSource = context.getDataSource(dataSourceName);

        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(QUERY_SQL)) {

            statement.setMaxRows(MAX_ROWS);

            for(Map<String, Object> row : request.getContents()) {
                Object keyword = row.get("keyword");
                statement.setString(1, "%" + (keyword == null ? "" : keyword) + "%");

                try (ResultSet resultSet = statement.executeQuery()) {
                    results.addAll(toRows(resultSet));
                }
            }
        }

        return PluginResult.success(results);
    }

    private static String dataSourceNameOf(PluginRequest request) {
        Object name = request.getFirstContent().get("dataSourceName");
        return (name == null || String.valueOf(name).isBlank()) ? DEFAULT_DATA_SOURCE_NAME : String.valueOf(name);
    }

    private static List<Map<String, Object>> toRows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<Map<String, Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for(int column = 1; column <= columnCount; column++) {
                Object value = resultSet.getObject(column);
                // 날짜 등 JDBC 타입은 응답 JSON 으로 그대로 직렬화되지 않을 수 있어 문자열로 바꾼다
                if(value instanceof java.util.Date || value instanceof java.time.temporal.Temporal) {
                    value = value.toString();
                }
                row.put(metaData.getColumnLabel(column), value);
            }
            rows.add(row);
        }
        return rows;
    }
}
