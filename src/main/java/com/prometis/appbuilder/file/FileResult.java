package com.prometis.appbuilder.file;

import com.prometis.appbuilder.file.code.FileResultCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder @AllArgsConstructor
public class FileResult {
    private FileResultCode resultCode;
    private String message;
    // 파일정보(filePath, fileName, fileSize, contentType, 조회 시 파일내용 등). 실패 시 null.
    private Map<String, Object> content;

    public static FileResult success(String message, Map<String, Object> content) {
        return FileResult.builder()
                .resultCode(FileResultCode.SUCCESS)
                .message(message)
                .content(content)
                .build();
    }

    public static FileResult failure(String message) {
        return FileResult.builder()
                .resultCode(FileResultCode.FAILURE)
                .message(message)
                .build();
    }
}
