package com.prometis.appbuilder.console.app.deploy.dto;

import com.prometis.appbuilder.console.app.deploy.dsl.DslIssue;

import java.util.List;

/**
 * 저장소 DSL 가져오기 미리보기.
 *
 * @param commitSha          비교한 커밋. 가져오기 요청에 그대로 보내면 이 커밋의 내용을 반영한다
 * @param sourceApplications 저장소(그 커밋)에 DSL 폴더가 있는 애플리케이션 ID 들
 * @param fileCount          저장소의 {sourceApplicationId}/ 아래 DSL 파일 수
 * @param issues             저장소 DSL 의 검증 결과 + 이 환경에 반영할 수 없는 문제. ERROR 가 있으면 가져올 수 없다
 */
public record DeployImportPreview(
        String branch,
        String repositoryUrl,
        String commitSha,
        String sourceApplicationId,
        List<String> sourceApplications,
        int fileCount,
        List<DeployImportChange> changes,
        List<DslIssue> issues,
        boolean importable
) {
}
