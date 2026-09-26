package com.prometis.appbuilder.app.executor.sql;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SqlQuery {
    private String sql;
    private List<SqlParameter> sqlParameters;

    public static SqlQuery createSqlQuery(String sql, List<SqlParameter> sqlParameters) {
        return SqlQuery.builder()
                        .sql(sql)
                        .sqlParameters(sqlParameters)
                        .build();
    }
}
