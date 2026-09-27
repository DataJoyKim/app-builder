package com.prometis.appbuilder.app.restclient;

import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServer;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServerRegister;
import com.prometis.appbuilder.app.executor.rest.HttpMethod;
import com.prometis.appbuilder.app.executor.rest.RestExecutor;
import com.prometis.appbuilder.app.executor.rest.RestExecutorRequest;
import com.prometis.appbuilder.app.executor.rest.RestExecutorResponse;
import com.prometis.appbuilder.app.restclient.code.BodyMessageFormat;
import com.prometis.appbuilder.app.restclient.code.ContentType;
import com.prometis.appbuilder.app.restclient.code.MessageDataType;
import com.prometis.appbuilder.app.restclient.code.ValueType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RestClient 정의로 만든 요청이 실제로 어떻게 나가는지 로컬 테스트 서버로 받아서 확인한다.
 * (외부 서버에 의존하지 않도록 JDK HttpServer 를 띄운다)
 */
class RestClientTest {
    private static final String APPLICATION_ID = "ehr";
    private static final String DATA_SOURCE = "testDataSource";

    private HttpServer server;

    // 테스트 서버가 받은 마지막 요청
    private String receivedMethod;
    private String receivedPath;
    private String receivedQuery;
    private String receivedHeader;
    private String receivedBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            receivedMethod = exchange.getRequestMethod();
            receivedPath = exchange.getRequestURI().getPath();
            receivedQuery = exchange.getRequestURI().getQuery();
            receivedHeader = exchange.getRequestHeaders().getFirst("test");
            receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            byte[] response = "{\"result\":\"OK\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        List<DataSourceRestServer> metadataList = new ArrayList<>();
        metadataList.add(DataSourceRestServer.builder()
                .applicationId(APPLICATION_ID)
                .dataSourceName(DATA_SOURCE)
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .connectTimeout(6000)
                .connectRequestTimeout(6000)
                .connectionMaxTotal(100)
                .connectionDefaultMaxPerRoute(100)
                .build());

        DataSourceRestServerRegister.initialize(metadataList);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    public void getTest(){
        // queryParams 설정
        List<RestClientQueryParam> queryParams = new ArrayList<>();
        queryParams.add(RestClientQueryParam.builder().paramName("codeKind").valueType(ValueType.PARAM_VALUE).build());

        // header 설정
        List<RestClientHeader> headers = new ArrayList<>();
        headers.add(RestClientHeader.builder().name("test").valueType(ValueType.INPUT_VALUE).inputValue("header-value").build());

        // 메타 정보 셋팅
        RestClient clientMeta = RestClient.builder()
                .applicationId(APPLICATION_ID)
                .clientName("testClient")
                .dataSourceName(DATA_SOURCE)
                .method(HttpMethod.GET)
                .path("/api/{version}/code")
                .queryParams(queryParams)
                .headers(headers)
                .build();

        RestExecutor executor = RestExecutor.createRestClientExecutor(clientMeta.getApplicationId(), clientMeta.getDataSourceName());

        // 요청파라미터
        Map<String, Object> p = new HashMap<>();
        p.put("codeKind", "CAR_COMPANY_CD");
        p.put("version","v1");

        RestClientRequest params = RestClientRequest.builder().params(p).build();

        RestExecutorRequest request = clientMeta.createRequest(params);

        // 요청
        RestExecutorResponse response =  executor.execute(request);

        assertEquals(200, response.getStatus().value());
        assertEquals(Map.of("result", "OK"), response.getBody());

        assertEquals("GET", receivedMethod);
        assertEquals("/api/v1/code", receivedPath);
        assertEquals("codeKind=CAR_COMPANY_CD", receivedQuery);
        assertEquals("header-value", receivedHeader);
    }

    @Test
    public void postTest(){
        List<RestClientBody> bodyMessage = new ArrayList<>();
        bodyMessage.add(RestClientBody.builder()
                .paramName("carAnnoId")
                .parentParamName("ROOT")
                .dataType(MessageDataType.STRING)
                .valueType(ValueType.PARAM_VALUE)
                .orderNum(1)
                .build());

        // 메타 정보 셋팅
        RestClient clientMeta = RestClient.builder()
                .applicationId(APPLICATION_ID)
                .clientName("testClient")
                .dataSourceName(DATA_SOURCE)
                .method(HttpMethod.POST)
                .path("/api/{version}/mypage/users/bookmark")
                .queryParams(new ArrayList<>())
                .bodyMessageFormat(BodyMessageFormat.OBJECT)
                .contentType(ContentType.APPLICATION_JSON)
                .headers(new ArrayList<>())
                .body(bodyMessage)
                .build();

        RestExecutor executor = RestExecutor.createRestClientExecutor(clientMeta.getApplicationId(), clientMeta.getDataSourceName());

        // 요청파라미터
        Map<String, Object> p = new HashMap<>();
        p.put("version","v1");

        Map<String, Object> body = new HashMap<>();
        body.put("carAnnoId","1321");

        RestClientRequest params = RestClientRequest.builder().params(p).requestBody(body).build();

        RestExecutorRequest request = clientMeta.createRequest(params);

        // 요청
        RestExecutorResponse response = executor.execute(request);

        assertEquals(200, response.getStatus().value());

        assertEquals("POST", receivedMethod);
        assertEquals("/api/v1/mypage/users/bookmark", receivedPath);
        assertEquals("{\"carAnnoId\":\"1321\"}", receivedBody);
    }
}
