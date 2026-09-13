package com.prometis.appbuilder.restapi.code;

import com.prometis.appbuilder.util.EnumStringConverter;

public enum HttpMethodType {
    GET, POST, PUT, DELETE;

    // GET 은 요청 본문을 받지 않는다.
    public boolean allowsBody() {
        return this != GET;
    }

    public static class Converter extends EnumStringConverter<HttpMethodType> {
        public Converter() {
            super(HttpMethodType.class);
        }
    }
}
