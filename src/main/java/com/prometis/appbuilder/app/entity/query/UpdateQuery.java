package com.prometis.appbuilder.app.entity.query;

import com.prometis.appbuilder.app.entity.EntityColumn;

import java.util.ArrayList;
import java.util.List;

public class UpdateQuery extends AbstractEntityQuery {
    @Override
    public String generateQuery(String tableName, List<EntityColumn> entityColumns) {
        List<EntityColumn> resolvedEntityColumns = resolveNull(entityColumns);

        List<EntityColumn> setColumns = new ArrayList<>();
        List<EntityColumn> keyColumns = new ArrayList<>();

        for(EntityColumn col : resolvedEntityColumns) {
            if(col.getUseKey()) {
                keyColumns.add(col);
            }
            else {
                setColumns.add(col);
            }
        }

        return "update " + tableName + System.lineSeparator() +
                "set " + System.lineSeparator() +
                updateSet(setColumns) +
                "where 1=1 " + System.lineSeparator() +
                where(keyColumns);
    }

    private String updateSet(List<EntityColumn> setColumns) {
        StringBuilder queryStr = new StringBuilder();

        int i=0;
        for(EntityColumn col : setColumns) {
            queryStr.append(tabSeparator)
                    .append(separator(i))
                    .append(col.getColumnName())
                    .append(" = ")
                    .append(parameter(col.getColumnName()))
                    .append(System.lineSeparator());
            i++;
        }

        return queryStr.toString();
    }

    private String where(List<EntityColumn> keyColumns) {
        StringBuilder queryStr = new StringBuilder();

        for(EntityColumn col : keyColumns) {
            queryStr.append(tabSeparator)
                    .append("and ")
                    .append(col.getColumnName())
                    .append(" = ")
                    .append(parameter(col.getColumnName()))
                    .append(System.lineSeparator());
        }

        return queryStr.toString();
    }
}
