package com.prometis.appbuilder.app.entity.query;

import com.prometis.appbuilder.app.entity.EntityColumn;
import com.prometis.appbuilder.app.entity.code.SelectWhereType;
import com.prometis.appbuilder.app.entity.code.SortOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SelectQuery extends AbstractEntityQuery {
    @Override
    public String generateQuery(String tableName, List<EntityColumn> entityColumns) {
        List<EntityColumn> resolvedEntityColumns = resolveNull(entityColumns);

        List<EntityColumn> whereColumns = new ArrayList<>();
        List<EntityColumn> orderColumns = new ArrayList<>();

        for(EntityColumn col : resolvedEntityColumns) {
            if(col.getSelectWhereType() != null) {
                whereColumns.add(col);
            }

            if(col.getSelectOrderByNum() != null) {
                orderColumns.add(col);
            }
        }

        orderColumns = orderColumns.stream()
                .sorted(Comparator.comparing(EntityColumn::getSelectOrderByNum, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        String sql = "";

        sql += "select " + System.lineSeparator();
        sql += selectColumns(resolvedEntityColumns);
        sql += "from " + tableName + System.lineSeparator();

        if(!whereColumns.isEmpty()) {
            sql += "where 1=1 " + System.lineSeparator();
            sql += where(whereColumns);
        }

        if(!orderColumns.isEmpty()) {
            sql += "order by" + System.lineSeparator();
            sql += orderBy(orderColumns);
        }

        sql += ";";

        return sql;
    }

    private String selectColumns(List<EntityColumn> columns) {
        StringBuilder queryStr = new StringBuilder();
        int i =0;
        for(EntityColumn col : columns) {
            queryStr.append("\t")
                    .append(separator(i))
                    .append(col.getColumnName())
                    .append(System.lineSeparator());
            i++;
        }
        return queryStr.toString();
    }

    private String where(List<EntityColumn> whereColumns) {
        StringBuilder queryStr = new StringBuilder();
        for(EntityColumn col : whereColumns) {
            if(SelectWhereType.COMPARE_REQUIRED.equals(col.getSelectWhereType())) {
                queryStr.append("\t")
                        .append("and ")
                        .append(col.getColumnName())
                        .append(" ").append(col.getSelectWhereCompareOperator()).append(" ")
                        .append(parameter(col.getColumnName()))
                        .append(System.lineSeparator());
            }
            else if(SelectWhereType.COMPARE_NOT_REQUIRED.equals(col.getSelectWhereType())) {
                queryStr.append("\t")
                        .append("and ")
                        .append("(")
                        .append(parameter(col.getColumnName()))
                        .append(" is null or ")
                        .append(col.getColumnName())
                        .append(" ").append(col.getSelectWhereCompareOperator()).append(" ")
                        .append(parameter(col.getColumnName()))
                        .append(")")
                        .append(System.lineSeparator());
            }
        }

        return queryStr.toString();
    }

    private String orderBy(List<EntityColumn> orderColumns) {
        StringBuilder queryStr = new StringBuilder();

        int i=0;
        for(EntityColumn col : orderColumns) {
            queryStr.append("\t")
                    .append(separator(i))
                    .append(col.getColumnName());

            if(SortOrder.ASC.equals(col.getSelectOrderBySortOrder())) {
                queryStr.append(" asc");
            }
            else if(SortOrder.DESC.equals(col.getSelectOrderBySortOrder())) {
                queryStr.append(" desc");
            }

            queryStr.append(System.lineSeparator());

            i++;
        }

        return queryStr.toString();
    }
}
