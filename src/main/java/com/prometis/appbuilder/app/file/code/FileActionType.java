package com.prometis.appbuilder.app.file.code;

import com.prometis.appbuilder.app.util.EnumStringConverter;

/**
 * File 노드의 액션유형.
 * READ   : 조회
 * UPLOAD : 업로드(첨부)
 * DELETE : 삭제
 */
public enum FileActionType {
    READ, UPLOAD, DELETE;

    public static class Converter extends EnumStringConverter<FileActionType> {
        public Converter() {
            super(FileActionType.class);
        }
    }
}
