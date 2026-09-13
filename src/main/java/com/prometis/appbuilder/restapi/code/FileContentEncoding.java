package com.prometis.appbuilder.restapi.code;

import com.prometis.appbuilder.util.EnumStringConverter;

/**
 * 파일내용 컬럼이 문자열일 때 어떻게 읽을지. byte[] / BLOB 값은 인코딩과 상관없이 그대로 쓴다.
 * BASE64 : Base64 로 인코딩된 문자열(data:...;base64, 접두어 허용)
 * TEXT   : 문자열 그대로(UTF-8)
 */
public enum FileContentEncoding {
    BASE64, TEXT;

    public static class Converter extends EnumStringConverter<FileContentEncoding> {
        public Converter() {
            super(FileContentEncoding.class);
        }
    }
}
