package com.prometis.appbuilder.app.executor.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SqlParameter {
    private String parameterName;
    private Integer parameterIndex;
    private Object value;

    public static SqlParameter createSqlParameter(
            String parameterName,
            Integer parameterIndex,
            Object value
    ) {
        return SqlParameter.builder()
                .parameterName(parameterName)
                .parameterIndex(parameterIndex)
                .value(value)
                .build();
    }
}
