package com.prometis.appbuilder.restapi.code;

import com.prometis.appbuilder.util.EnumStringConverter;

/**
 * 파라미터를 요청의 어느 부분에서 읽는지(또는 응답의 어디에 쓰는지).
 * PATH     : 경로의 {name} 자리 값
 * QUERY    : Request Param (?name=value)
 * HEADER   : Request Header
 * BODY     : Request Body(JSON) 의 필드
 * RESPONSE : JSON 응답의 contents. 최상위는 응답할 메시지, 그 아래는 메시지 행의 응답스키마다.
 */
public enum ParameterIn {
    PATH, QUERY, HEADER, BODY, RESPONSE;

    // 요청에서 값을 하나씩 읽는 위치(PATH/QUERY/HEADER). 트리를 이루는 BODY/RESPONSE 와 구분한다.
    public boolean isFlatRequest() {
        return this == PATH || this == QUERY || this == HEADER;
    }

    public static class Converter extends EnumStringConverter<ParameterIn> {
        public Converter() {
            super(ParameterIn.class);
        }
    }
}
