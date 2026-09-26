package com.prometis.appbuilder.app.restapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 콘솔에서 정의한 REST API 의 실제 진입점. 경로별 핸들러를 따로 두지 않고 URL_PREFIX/** 를 한 곳에서 받아 정의를 찾아 실행한다.
 */
@RestController
@RequiredArgsConstructor
public class RestApiController {
    private final RestApiExecuteService restApiExecuteService;

    @RequestMapping(
            value = {RestApi.URL_PREFIX, RestApi.URL_PREFIX + "/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}
    )
    public ResponseEntity<?> handle(
            HttpServletRequest request,
            HttpServletResponse response,
            // 본문은 JSON 이 깨져 있어도 스키마 에러로 알려주기 위해 문자열로 받아 직접 파싱한다.
            @RequestBody(required = false) String body
    ) {
        RestApiResult result = restApiExecuteService.execute(request, response, body);

        if(result instanceof RestApiResult.File file) {
            RestApiResponseMapper.FileContent content = file.content();

            // 한글 파일명이 깨지지 않도록 filename*=UTF-8'' 형식으로 내려준다.
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(content.fileName(), StandardCharsets.UTF_8)
                            .build()
                            .toString())
                    .contentType(content.contentType())
                    .contentLength(content.bytes().length)
                    .body(content.bytes());
        }

        RestApiResult.Json json = (RestApiResult.Json) result;

        return ResponseEntity.status(json.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.body());
    }
}
