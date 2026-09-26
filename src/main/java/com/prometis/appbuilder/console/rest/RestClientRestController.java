package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.executor.rest.HttpMethod;
import com.prometis.appbuilder.app.restclient.*;
import com.prometis.appbuilder.app.restclient.code.BodyMessageFormat;
import com.prometis.appbuilder.app.restclient.code.ContentType;
import com.prometis.appbuilder.app.restclient.code.MessageDataType;
import com.prometis.appbuilder.app.restclient.code.ValueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.RestClientRestController")
@RequestMapping("/console/api/rest-client")
public class RestClientRestController {
    @Autowired
    private RestClientRepository restClientRepository;
    @Autowired
    private RestClientService restClientService;

    @GetMapping("")
    public ResponseEntity<?> getRestClient(
            @RequestParam(name = "clientName", required = false) String clientName
    ) {
        List<RestClient> results;
        if(clientName != null) {
            Optional<RestClient> restClient = restClientRepository.findByClientName(clientName);
            results = new ArrayList<>();
            restClient.ifPresent(results::add);
        }
        else {
            results = restClientRepository.findAll();
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRestClient(@PathVariable("id") Long id) {
        RestClient results = restClientRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Map<String,Object> params) {
        Map<String,Object> pRestClient = (Map<String, Object>) params.get("restClient");
        List<Map<String,Object>> pQueryParams = (List<Map<String,Object>>) params.get("queryParams");
        List<Map<String,Object>> pHeaders = (List<Map<String,Object>>) params.get("headers");
        List<Map<String,Object>> pBody = (List<Map<String,Object>>) params.get("body");

        Long id = (pRestClient.get("id") == null || ((String) pRestClient.get("id")).isEmpty())
                ? null
                : Long.valueOf((String) pRestClient.get("id"));

        List<RestClientQueryParam> queryParams = new ArrayList<>();
        for(Map<String,Object> p : pQueryParams) {
            queryParams.add(
                    RestClientQueryParam.builder()
                            .paramName((String) p.get("paramName"))
                            .valueType(ValueType.valueOf((String) p.get("valueType")))
                            .inputValue((String) p.get("inputValue"))
                            .build());
        }

        List<RestClientHeader> headers = new ArrayList<>();
        for(Map<String,Object> p : pHeaders) {
            headers.add(
                    RestClientHeader.builder()
                            .name((String) p.get("name"))
                            .valueType(ValueType.valueOf((String) p.get("valueType")))
                            .inputValue((String) p.get("inputValue"))
                            .build());
        }

        List<RestClientBody> body = new ArrayList<>();
        for(Map<String,Object> p : pBody) {
            body.add(
                    RestClientBody.builder()
                            .paramName((String) p.get("paramName"))
                            .parentParamName((String) p.get("parentParamName"))
                            .dataType(MessageDataType.valueOf((String) p.get("dataType")))
                            .valueType(ValueType.valueOf((String) p.get("valueType")))
                            .orderNum((Integer) p.get("orderNum"))
                            .inputValue((String) p.get("inputValue"))
                            .build());
        }

        Object contentType = pRestClient.get("contentType");
        Object bodyMessageFormat = pRestClient.get("bodyMessageFormat");
        RestClient restClient;
        if (id == null) {
            restClient = RestClient.builder()
                    .clientName((String) pRestClient.get("clientName"))
                    .displayName((String) pRestClient.get("displayName"))
                    .dataSourceName((String) pRestClient.get("dataSourceName"))
                    .method(HttpMethod.valueOf((String) pRestClient.get("method")))
                    .path((String) pRestClient.get("path"))
                    .contentType((contentType != null) ? ContentType.valueOf((String) contentType) : null)
                    .bodyMessageFormat((bodyMessageFormat != null) ? BodyMessageFormat.valueOf((String) bodyMessageFormat) : null)
                    .queryParams(queryParams)
                    .headers(headers)
                    .body(body)
                    .build();
        }
        else {
            restClient = restClientRepository.findById(id)
                    .orElseThrow(RuntimeException::new);

            restClient.update(
                    (String) pRestClient.get("clientName"),
                    (String) pRestClient.get("displayName"),
                    (String) pRestClient.get("dataSourceName"),
                    HttpMethod.valueOf((String) pRestClient.get("method")),
                    (String) pRestClient.get("path"),
                    (contentType != null) ? ContentType.valueOf((String) contentType) : null,
                    (bodyMessageFormat != null) ? BodyMessageFormat.valueOf((String) bodyMessageFormat) : null,
                    queryParams,
                    headers,
                    body
            );
        }

        RestClient results = restClientRepository.save(restClient);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRestClient(@PathVariable("id") Long id) {
        RestClient restClient = restClientRepository.findById(id)
                .orElseThrow(RuntimeException::new);

        restClientRepository.deleteById(restClient.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{clientName}/execute")
    public ResponseEntity<?> executeQuery(
            @PathVariable("clientName") String clientName,
            @RequestParam Map<String, Object> params,
            @RequestBody(required = false) Object requestBody
    ) {
        RestClientRequest request = RestClientRequest.builder()
                .params(params)
                .requestBody(requestBody)
                .build();

        RestClientResult results = restClientService.execute(clientName, request);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
