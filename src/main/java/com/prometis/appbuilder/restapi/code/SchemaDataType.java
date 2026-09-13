package com.prometis.appbuilder.restapi.code;

import com.prometis.appbuilder.util.EnumStringConverter;

public enum SchemaDataType {
    STRING, INTEGER, NUMBER, BOOLEAN, OBJECT, ARRAY;

    public boolean isContainer() {
        return this == OBJECT || this == ARRAY;
    }

    public static class Converter extends EnumStringConverter<SchemaDataType> {
        public Converter() {
            super(SchemaDataType.class);
        }
    }
}
