package com.prometis.appbuilder.restapi.code;

import com.prometis.appbuilder.util.EnumStringConverter;

/**
 * JSON : 워크플로우 응답을 JSON 으로 돌려준다. 응답메시지 선택/응답스키마를 적용할 수 있다.
 * FILE : 응답메시지 한 행에 담긴 파일 데이터를 첨부파일로 내려준다.
 */
public enum ResponseType {
    JSON, FILE;

    public static class Converter extends EnumStringConverter<ResponseType> {
        public Converter() {
            super(ResponseType.class);
        }
    }
}
