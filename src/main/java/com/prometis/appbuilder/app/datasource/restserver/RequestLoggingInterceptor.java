package com.prometis.appbuilder.app.datasource.restserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@Slf4j
public class RequestLoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        log.info("requestURL: {}", request.getURI());
        log.info("requestMethod: {}", request.getMethod());
        log.info("requestHeaders: {}",request.getHeaders());


        return execution.execute(request, body);
    }
}
