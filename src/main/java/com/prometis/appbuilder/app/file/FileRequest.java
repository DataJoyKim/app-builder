package com.prometis.appbuilder.app.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Getter
@AllArgsConstructor @Builder
public class FileRequest {
    private Map<String, Object> params;
    private MultipartFile multipartFile;
}
