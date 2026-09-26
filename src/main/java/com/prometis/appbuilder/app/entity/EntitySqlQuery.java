package com.prometis.appbuilder.app.entity;

import com.prometis.appbuilder.app.executor.sql.SqlParameter;
import com.prometis.appbuilder.app.executor.sql.SqlQuery;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @Builder
public class EntitySqlQuery {
    private String seq;
    private SqlQuery sqlQuery;

    public static EntitySqlQuery createEntitySqlQuery(String seq, String sql, List<SqlParameter> sqlParameters) {
        return EntitySqlQuery.builder()
                .seq(seq)
                .sqlQuery(SqlQuery.builder()
                        .sql(sql)
                        .sqlParameters(sqlParameters)
                        .build())
                .build();
    }
}
