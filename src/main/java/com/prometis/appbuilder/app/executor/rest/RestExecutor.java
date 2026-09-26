package com.prometis.appbuilder.app.executor.rest;

import com.prometis.appbuilder.app.datasource.LookupKey;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServerRegister;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class RestExecutor {
    private final RestClient restClient;

    public RestExecutorResponse execute(RestExecutorRequest request) {

        ResponseEntity<String> response = null;

        if(HttpMethod.GET.equals(request.getMethod())) {
            response = restClient.get()
                    .uri(request::createUri)
                    .headers(request::createHeaders)
                    .retrieve()
                    .toEntity(String.class);
        }
        else if(HttpMethod.POST.equals(request.getMethod())) {
            response = restClient.post()
                    .uri(request::createUri)
                    .headers(request::createHeaders)
                    .contentType(request.getMediaType())
                    .body(request.getRequestBody())
                    .retrieve()
                    .toEntity(String.class);
        }
        else if(HttpMethod.PUT.equals(request.getMethod())) {
            response = restClient.put()
                    .uri(request::createUri)
                    .headers(request::createHeaders)
                    .contentType(request.getMediaType())
                    .body(request.getRequestBody())
                    .retrieve()
                    .toEntity(String.class);
        }
        else if(HttpMethod.DELETE.equals(request.getMethod())) {
            response = restClient.delete()
                    .uri(request::createUri)
                    .headers(request::createHeaders)
                    .retrieve()
                    .toEntity(String.class);
        }

        assert response != null;

        Object body = formatting(response);

        return RestExecutorResponse.builder()
                .status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(body)
                .build();
    }

    public static RestExecutor createRestClientExecutor(String dataSourceName) {
        RestClient restClient = DataSourceRestServerRegister.getDataSource(LookupKey.generateKey(dataSourceName));

        return new RestExecutor(restClient);
    }

    private static Object formatting(ResponseEntity<String> response) {
        MediaType contentType = response.getHeaders().getContentType();

        Object body;
        if (MediaType.APPLICATION_JSON.includes(contentType)) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                body = objectMapper.readValue(response.getBody(), Object.class);
            }
            catch (JsonProcessingException e) {
                log.error("error",e);
                body = response.getBody();
            }
        }
        else if (MediaType.TEXT_PLAIN.includes(contentType) || MediaType.TEXT_HTML.includes(contentType)) {
            body = response.getBody();
        }
        else {
            body = response.getBody();
        }

        return body;
    }
}
