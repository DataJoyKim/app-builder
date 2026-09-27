package com.prometis.appbuilder.app.dto;

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

        // 요청 URL(/{applicationId}/...)에서 서버가 채운다. 노드 실행기가 쿼리/클라이언트/데이터소스 등을 이 애플리케이션 것으로 찾는 데 쓴다.
        // 클라이언트가 본문으로 다른 애플리케이션을 지정하지 못하도록 JSON 으로는 받지도 내보내지도 않는다.
        @JsonIgnore
        private String applicationId;

        // multipart/form-data 로 올라온 업로드 파일.
        @JsonIgnore
        private List<MultipartFile> files;
    }
}
