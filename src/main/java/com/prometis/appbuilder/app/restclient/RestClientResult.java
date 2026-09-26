package com.prometis.appbuilder.app.restclient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

@Getter @AllArgsConstructor @Builder
public class RestClientResult {
    private HttpStatusCode statusCode;
    private HttpHeaders headers;
    private Object body;
}
