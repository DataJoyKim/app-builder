package com.prometis.appbuilder.console.app.deploy.dto;

import java.util.List;

/**
 * 가져오기 결과. warnings 는 DB 에는 반영됐지만 따로 확인할 일(데이터소스 새로고침, 화면 소스 쓰기 실패 등)이다.
 */
public record DeployImportResult(
        String commitSha,
        String sourceApplicationId,
        int created,
        int updated,
        int deleted,
        List<String> warnings
) {
}
