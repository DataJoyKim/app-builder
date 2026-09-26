package com.prometis.appbuilder.app.restclient;

import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServer;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServerRegister;
import com.prometis.appbuilder.app.executor.rest.HttpMethod;
import com.prometis.appbuilder.app.executor.rest.RestExecutor;
import com.prometis.appbuilder.app.executor.rest.RestExecutorRequest;
import com.prometis.appbuilder.app.executor.rest.RestExecutorResponse;
import com.prometis.appbuilder.app.restclient.*;
import com.prometis.appbuilder.app.restclient.code.BodyMessageFormat;
import com.prometis.appbuilder.app.restclient.code.ContentType;
import com.prometis.appbuilder.app.restclient.code.ValueType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RestClientTest {

    private void setDataSource() {
        List<DataSourceRestServer> metadataList = new ArrayList<>();
        metadataList.add(DataSourceRestServer.builder()
                .dataSourceName("testDataSource")
                .baseUrl("https://career.netmarble.com")
                .connectTimeout(6000)
                .connectRequestTimeout(6000)
                .connectionMaxTotal(100)
                .connectionDefaultMaxPerRoute(100)
                .build());

        DataSourceRestServerRegister.initialize(metadataList);
    }

    @Test
    public void getTest(){
        setDataSource();

        // queryParams 설정
        List<RestClientQueryParam> queryParams = new ArrayList<>();
        queryParams.add(RestClientQueryParam.builder().paramName("codeKind").valueType(ValueType.PARAM_VALUE).build());

        // header 설정
        List<RestClientHeader> headers = new ArrayList<>();
        headers.add(RestClientHeader.builder().name("test").valueType(ValueType.PARAM_VALUE).build());
        headers.add(RestClientHeader.builder().name("Cookie").valueType(ValueType.PARAM_VALUE).build());

        // 메타 정보 셋팅
        RestClient clientMeta = RestClient.builder()
                .clientName("testClient")
                .dataSourceName("testDataSource")
                .method(HttpMethod.GET)
                .path("/api/{version}/code")
                .queryParams(queryParams)
                .headers(headers)
                .build();

        RestExecutor executor = RestExecutor.createRestClientExecutor(clientMeta.getDataSourceName());

        // 요청파라미터
        Map<String, Object> p = new HashMap<>();
        p.put("codeKind", "CAR_COMPANY_CD");
        p.put("version","v1");

        RestClientRequest params = RestClientRequest.builder().params(p).build();

        RestExecutorRequest request = clientMeta.createRequest(params);

        // 요청
        RestExecutorResponse response =  executor.execute(request);

        Assertions.assertNotNull(response);
    }


    @Test
    public void postTest(){
        setDataSource();

        // queryParams 설정
        List<RestClientQueryParam> queryParams = new ArrayList<>();
        //queryParams.add(RestClientQueryParam.builder().parameterName("codeKind").build());

        List<RestClientBody> bodyMessage = new ArrayList<>();
        bodyMessage.add(RestClientBody.builder().paramName("carAnnoId").parentParamName("ROOT").orderNum(1).build());

        // header 설정
        List<RestClientHeader> headers = new ArrayList<>();
        // headers.add(RestClientHeader.builder().key("Cookie").value("CAR=zJpsEzlVF6bjdxpOiKDE20FFpBtcykPGX51ZGaf0CHv/ZOPGEP6hsePQYXU3I1xJBlz6h36g7xPZSQ6NT/gJCDCGLb8eyoHlFsMEbNTeO4cxP4czry+x+N7mfMV4e4rdqQanBgUdsWnOp4T55q20X/hUJHs1QffR4eAY7MWAvLQ+InWfPJpNfXi7v4dFPkaRESREoNYR3nKsl8VqPphR3faWxHdw57DMvP7yWHeemM7JX5otMaEcs0v9LPlNIqfS4kv4ercdT6WqLdcF2RfgPKqLSp2dLmqQyx+kI3ifgrs2hWZ/baLIg3zgXSu/ttkmLT4SNIkbdcCMVa4wi2i1waxOUtEwMoyHqcqWaf85gMovQOZFXT7B3ZSHyEyb2OO1YcJqwbqzbWo/fOFJkQ+QS/2CpBjgMFNlaUraJJr0QRWNSEX1tSo75DYBG4DwviGXkckKSpffn/27AAOPUiAnSSN49rL5dy1Z//e8PqBhZdCb7T/u4O63OrAd4q2dAyUItn8ALmOwVMBxbvfYK8SIEw==").build());

        // 메타 정보 셋팅
        RestClient clientMeta = RestClient.builder()
                .clientName("testClient")
                .dataSourceName("testDataSource")
                .method(HttpMethod.POST)
                .path("/api/{version}/mypage/users/bookmark")
                .queryParams(queryParams)
                .bodyMessageFormat(BodyMessageFormat.OBJECT)
                .contentType(ContentType.APPLICATION_JSON)
                .headers(headers)
                .body(bodyMessage)
                .build();

        RestExecutor executor = RestExecutor.createRestClientExecutor(clientMeta.getDataSourceName());

        // 요청파라미터
        Map<String, Object> p = new HashMap<>();
        p.put("version","v1");

        Map<String, Object> body = new HashMap<>();
        body.put("carAnnoId","1321");

        RestClientRequest params = RestClientRequest.builder().params(p).requestBody(body).build();

        RestExecutorRequest request = clientMeta.createRequest(params);

        // 요청
        RestExecutorResponse response = executor.execute(request);

        System.out.println(response);
    }
}