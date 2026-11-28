package com.prometis.appbuilder.console.app.deploy.dto;

import java.util.List;

/**
 * 저장소 DSL 배포 요청.
 *
 * @param branch              배포할 브랜치. 비우면 연동 설정의 브랜치
 * @param sourceApplicationId 저장소에서 읽을 애플리케이션 폴더({sourceApplicationId}/...). 비우면 지금 애플리케이션
 * @param commitSha           배포(apply)에서만: 미리보기한 커밋. 그 커밋의 내용을 그대로 반영한다
 * @param keys                배포(apply)에서만: 반영할 메타데이터(DslImportPlanner.keyOf = kind/id). null 이면 바뀐 것 전부
 */
public record DeployImportRequest(String branch, String sourceApplicationId, String commitSha, List<String> keys) {
}
