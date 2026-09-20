package com.prometis.appbuilder.restapi.code;

import com.prometis.appbuilder.util.EnumStringConverter;

/**
 * 파일내용 값을 어떻게 읽을지. byte[] / BLOB / MultipartFile 값은 인코딩과 상관없이 그대로 쓴다.
 * BASE64    : Base64 로 인코딩된 문자열(data:...;base64, 접두어 허용)
 * TEXT      : 문자열 그대로(UTF-8)
 * MULTIPART : multipart/form-data 로 업로드된 파일(MultipartFile) 자체. 업로드(요청)에서만 쓸 수 있다.
 */
public enum FileContentEncoding {
    BASE64, TEXT, MULTIPART;

    // 응답(파일 다운로드)에는 쓸 수 없는 인코딩인지.
    public boolean isRequestOnly() {
        return this == MULTIPART;
    }

    public static class Converter extends EnumStringConverter<FileContentEncoding> {
        public Converter() {
            super(FileContentEncoding.class);
        }
    }
}
