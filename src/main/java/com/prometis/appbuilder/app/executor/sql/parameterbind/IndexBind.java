package com.prometis.appbuilder.app.executor.sql.parameterbind;

import com.prometis.appbuilder.app.executor.sql.SqlQuery;

public class IndexBind implements ParameterBinder {
    @Override
    public SqlQuery binding(SqlQuery sqlQuery) {
        return sqlQuery;
    }
}
