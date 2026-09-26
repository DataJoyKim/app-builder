package com.prometis.appbuilder.app.executor.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

@Getter @AllArgsConstructor @Builder
public class RestExecutorResponse {
    private HttpHeaders headers;
    private Object body;
    private HttpStatusCode status;
}
