package com.prometis.appbuilder.app.executor.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @AllArgsConstructor @Builder
public class StoredFile {
    // 저장소경로 기준 상대경로 ('/' 구분)
    private String filePath;
    private String fileName;
    private long fileSize;
    private String contentType;
    // read 일 때만 채워진다.
    private byte[] content;
}
