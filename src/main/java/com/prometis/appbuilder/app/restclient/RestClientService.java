package com.prometis.appbuilder.app.restclient;

import com.prometis.appbuilder.app.executor.rest.RestExecutor;
import com.prometis.appbuilder.app.executor.rest.RestExecutorRequest;
import com.prometis.appbuilder.app.executor.rest.RestExecutorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
public class RestClientService {
    private final RestClientRepository restClientRepository;

    public RestClientResult execute(String clientName, RestClientRequest params) {
        RestClient clientMeta = restClientRepository.findByClientName(clientName)
                            .orElseThrow();

        RestExecutorRequest request = clientMeta.createRequest(params);

        try {
            RestExecutor restExecutor = RestExecutor.createRestClientExecutor(clientMeta.getDataSourceName());

            RestExecutorResponse response = restExecutor.execute(request);

            return RestClientResult.builder()
                    .headers(response.getHeaders())
                    .body(response.getBody())
                    .statusCode(response.getStatus())
                    .build();
        }
        catch (HttpClientErrorException e) {
            return RestClientResult.builder()
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString())
                    .statusCode(e.getStatusCode())
                    .build();
        }
    }
}
