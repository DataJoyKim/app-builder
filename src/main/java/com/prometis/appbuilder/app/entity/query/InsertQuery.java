package com.prometis.appbuilder.app.entity.query;

import com.prometis.appbuilder.app.entity.EntityColumn;

import java.util.List;

public class InsertQuery extends AbstractEntityQuery {

    @Override
    public String generateQuery(String tableName, List<EntityColumn> entityColumns) {
        List<EntityColumn> resolvedEntityColumns = resolveNull(entityColumns);

        return "insert " + tableName + " ( " + System.lineSeparator() +
                columns(resolvedEntityColumns) +
                ") values (" + System.lineSeparator() +
                params(resolvedEntityColumns) +
                ")";
    }

    private String columns(List<EntityColumn> entityColumns) {
        StringBuilder queryStr = new StringBuilder();

        int i=0;
        for(EntityColumn col : entityColumns) {
            queryStr.append(tabSeparator)
                    .append(separator(i))
                    .append(col.getColumnName())
                    .append(System.lineSeparator());
            i++;
        }

        return queryStr.toString();
    }

    private String params(List<EntityColumn> entityColumns) {
        StringBuilder queryStr = new StringBuilder();

        int i=0;
        for(EntityColumn col : entityColumns) {
            queryStr.append(tabSeparator)
                    .append(separator(i))
                    .append(parameter(col.getColumnName()))
                    .append(System.lineSeparator());
            i++;
        }

        return queryStr.toString();
    }
}
