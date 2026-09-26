package com.prometis.appbuilder.app.entity.query;

import com.prometis.appbuilder.app.entity.EntityColumn;
import com.prometis.appbuilder.app.entity.code.NullResolveType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractEntityQuery implements EntityQueryGenerator {

    protected String tabSeparator = "\t";

    @Override
    public String generate(String tableName, List<EntityColumn> entityColumns) {
        return generateQuery(tableName, entityColumns);
    }

    protected abstract String generateQuery(String tableName, List<EntityColumn> entityColumns) ;

    protected static String separator(int i) {
        return (i > 0) ? "," : "";
    }

    protected String parameter(String columnName) {
        return "#{" + columnName + "}";
    }

    protected List<EntityColumn> resolveNull(List<EntityColumn> entityColumns) {
        List<EntityColumn> resolvedEntityColumns = new ArrayList<>();

        for(EntityColumn col : entityColumns) {
            if(col.getValue() != null) {
                resolvedEntityColumns.add(col);
                continue;
            }

            if(NullResolveType.SET_NULL.equals(col.getInsertNullResolveType())) {
                resolvedEntityColumns.add(col);
            }
            else if(NullResolveType.EXCEPTION.equals(col.getInsertNullResolveType())) {
                throw new EntityParamNullException();
            }
            else if(NullResolveType.EXCLUDE_QUERY.equals(col.getInsertNullResolveType())) {
                log.info("column[{}] excluded query.", col.getColumnName());
            }
            else {
                resolvedEntityColumns.add(col);
            }
        }
        return resolvedEntityColumns;
    }
}
