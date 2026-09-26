package com.prometis.appbuilder.app.executor.sql.parameterbind;

import com.prometis.appbuilder.app.executor.sql.SqlQuery;

public interface ParameterBinder {
    SqlQuery binding(SqlQuery sqlQuery);
}
