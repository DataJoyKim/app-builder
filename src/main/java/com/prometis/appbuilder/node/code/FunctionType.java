package com.prometis.appbuilder.node.code;

import com.prometis.appbuilder.util.EnumStringConverter;

public enum FunctionType {
    ENTITY, SQL, REST_CLIENT, MESSAGE_PROCESSOR, NOTIFICATION, FILE, CONDITION, ERROR_MESSAGE;

    public static class Converter extends EnumStringConverter<FunctionType> {
        public Converter() {
            super(FunctionType.class);
        }
    }
}
