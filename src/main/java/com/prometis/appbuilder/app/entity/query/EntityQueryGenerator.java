package com.prometis.appbuilder.app.entity.query;

import com.prometis.appbuilder.app.entity.EntityColumn;

import java.util.List;

public interface EntityQueryGenerator {

    String generate(String tableName, List<EntityColumn> entityColumns);
}
