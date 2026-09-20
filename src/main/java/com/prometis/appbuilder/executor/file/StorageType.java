package com.prometis.appbuilder.executor.file;

import com.prometis.appbuilder.util.EnumStringConverter;

/**
 * 파일저장소 유형. 유형마다 설정(options)이 다르다.
 * LOCAL  : 서버 로컬 디스크. 설정 = rootPath(저장소경로)
 * REMOTE : 추후 제공 예정.
 */
public enum StorageType {
    LOCAL;

    public static class Converter extends EnumStringConverter<StorageType> {
        public Converter() {
            super(StorageType.class);
        }
    }
}
