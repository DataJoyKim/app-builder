package com.prometis.appbuilder.app.query;

import com.prometis.appbuilder.app.query.code.QueryResultCode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
@Getter
@Builder
public class QueryResult {
    private QueryResultCode resultCode;
    private List<Map<String, Object>> results;
    private String failedMessage;
    public static QueryResult createQueryResult(QueryResultCode resultCode, List<Map<String, Object>> results) {
        return QueryResult.builder()
                .results(results)
                .resultCode(resultCode)
                .build();
    }

    public static QueryResult createFailedMessage(String message) {
        return QueryResult.builder()
                .resultCode(QueryResultCode.FAILURE)
                .failedMessage(message)
                .build();
    }
}
