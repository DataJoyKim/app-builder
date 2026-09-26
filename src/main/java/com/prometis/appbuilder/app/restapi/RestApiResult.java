package com.prometis.appbuilder.app.restapi;

/**
 * REST API 실행 결과. JSON 본문으로 응답하거나, 파일로 내려준다.
 */
public sealed interface RestApiResult permits RestApiResult.Json, RestApiResult.File {

    record Json(int status, Object body) implements RestApiResult {
    }

    record File(RestApiResponseMapper.FileContent content) implements RestApiResult {
    }
}
