package com.prometis.appbuilder.app.datasource.database.schema;

import com.prometis.appbuilder.app.entity.code.ColumnType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColumnSchema {
    private String columnName;
    private String dataType;
    private ColumnType columnType;
    private Boolean primaryKey;
    private String comment;
}
