package com.prometis.appbuilder.app.executor.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Map;

@Getter @AllArgsConstructor @Builder
public class RestExecutorRequest {
    private HttpMethod method;
    private String path;
    private Object requestBody;
    private Map<String, Object> requestPathVariable;
    private Map<String, Object> requestQueryParam;
    private Map<String, String> requestHeaders;
    private MediaType mediaType;

    public URI createUri(UriBuilder uriBuilder) {
        uriBuilder.path(this.path);

        for(String key : this.requestQueryParam.keySet()) {
            uriBuilder.queryParam(key, this.requestQueryParam.get(key));
        }
        return uriBuilder.build(this.requestPathVariable);
    }

    public void createHeaders(HttpHeaders headers) {
        if(headers == null) {
            return;
        }

        for(String key : this.requestHeaders.keySet()) {
            headers.set(key, String.valueOf(this.requestHeaders.get(key)));
        }
    }
}
