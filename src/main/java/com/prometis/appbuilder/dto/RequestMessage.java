package com.prometis.appbuilder.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Data
public class RequestMessage {
    private Map<String, List<Map<String, Object>>> body;
    private Header header;

    @Data
    public static class Header {
        private String workflowCode;
        private String objectCode;
        private String localeCode;

        // multipart/form-data 로 올라온 업로드 파일.
        @JsonIgnore
        private List<MultipartFile> files;
    }
}
