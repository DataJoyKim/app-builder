package com.prometis.appbuilder.app.executor.sql.parameterbind;

import com.prometis.appbuilder.app.executor.sql.SqlParameter;
import com.prometis.appbuilder.app.executor.sql.SqlQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class NameBind implements ParameterBinder {
    private static final String separatorPrefix = "#\\{";
    private static final String separatorPostfix = "\\}";
    private static final String indexBindVariable = "?";

    @Override
    public SqlQuery binding(SqlQuery sqlQuery) {
        if(sqlQuery == null || sqlQuery.getSql() == null) {
            throw new IllegalArgumentException();
        }

        List<String> extractedParameters = extractParameter(sqlQuery.getSql());

        Set<String> removedDuplicateParameters = new HashSet<>(extractedParameters);

        String convertedSql = convertSql(sqlQuery.getSql(), removedDuplicateParameters);

        Map<String, SqlParameter> sqlParameterMap = toSqlParameterMap(sqlQuery.getSqlParameters());

        List<SqlParameter> convertedSqlParameters = convertSqlParameters(extractedParameters, sqlParameterMap);

        return SqlQuery.builder()
                .sql(convertedSql)
                .sqlParameters(convertedSqlParameters)
                .build();
    }

    private List<SqlParameter> convertSqlParameters(List<String> extractedParameters, Map<String, SqlParameter> sqlParameterMap) {
        List<SqlParameter> convertedSqlParameters = new ArrayList<>();

        int index = 1;
        for(String parameterName : extractedParameters) {
            if(!sqlParameterMap.containsKey(parameterName)) {
                continue;
            }

            SqlParameter sqlParameter = sqlParameterMap.get(parameterName);

            convertedSqlParameters.add(
                    SqlParameter.createSqlParameter(
                            parameterName,
                            index,
                            sqlParameter.getValue()
                    ));

            index++;
        }

        return convertedSqlParameters;
    }

    private static Map<String, SqlParameter> toSqlParameterMap(List<SqlParameter> sqlParameters) {
        if(sqlParameters == null) {
            return new HashMap<>();
        }

        Map<String, SqlParameter> sqlParameterMap = new HashMap<>();
        for(SqlParameter sqlParameter : sqlParameters) {
            sqlParameterMap.put(sqlParameter.getParameterName(), sqlParameter);
        }
        return sqlParameterMap;
    }

    private String convertSql(String sql, Set<String> removedDuplicateParameters) {
        return sql.replaceAll(getSeparatorParameterName(), indexBindVariable);
    }

    private List<String> extractParameter(String sql) {
        Pattern pattern = Pattern.compile(getSeparatorParameterName());
        Matcher matcher = pattern.matcher(sql);

        List<String> extractedParameters = new ArrayList<>();

        while (matcher.find()) {
            extractedParameters.add(matcher.group(1));
        }
        return extractedParameters;
    }

    private String getSeparatorParameterName() {
        return separatorPrefix + "(\\w+)" + separatorPostfix;
    }
}
