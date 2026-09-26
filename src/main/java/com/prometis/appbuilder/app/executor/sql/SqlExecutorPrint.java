package com.prometis.appbuilder.app.executor.sql;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlExecutorPrint {
    public static void print(String sqlId, SqlQuery sqlQuery) {

        StringBuilder print = new StringBuilder();

        print.append(System.lineSeparator());
        print.append("=============================================")
                .append(System.lineSeparator());
        print.append("[SQL] ").append(sqlId)
                .append(System.lineSeparator());
        print.append("---------------------------------------------")
                .append(System.lineSeparator());
        print.append(sqlQuery.getSql())
                .append(System.lineSeparator());
        print.append("---------------------------------------------")
                .append(System.lineSeparator());
        print.append("[Parameter]")
                .append(System.lineSeparator());
        print.append("---------------------------------------------")
                .append(System.lineSeparator());

        for (SqlParameter sqlParameter : sqlQuery.getSqlParameters()) {
            print.append("> ")
                    .append(sqlParameter.getParameterName())
                    .append(" | ")
                    .append(sqlParameter.getParameterIndex())
                    .append(" | ")
                    .append(sqlParameter.getValue())
                    .append(System.lineSeparator());
        }

        print.append("=============================================");

        log.info(print.toString());
    }
}
