package com.prometis.appbuilder.app.executor.sql.parameterbind;

public class ParameterBinderFactory {

    public static ParameterBinder instance(ParameterBindType parameterBindType) {
        if(parameterBindType == null) {
            return new NameBind();
        }

        if(ParameterBindType.NAME_BIND.equals(parameterBindType)){
            return new NameBind();
        }
        else if(ParameterBindType.INDEX_BIND.equals(parameterBindType)) {
            return new IndexBind();
        }
        else {
            return null;
        }
    }
}
