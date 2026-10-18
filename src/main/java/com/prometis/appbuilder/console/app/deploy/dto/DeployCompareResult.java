package com.prometis.appbuilder.console.app.deploy.dto;

import com.prometis.appbuilder.console.app.deploy.dsl.DslIssue;

import java.util.List;

/**
 * 저장소 비교 결과.
 *
 * @param baseCommitSha 비교한 브랜치의 커밋. 커밋할 때 그대로 돌려보내서 그 사이 브랜치가 바뀌지 않았는지 확인한다. 브랜치가 아직 없으면 null
 * @param fileCount 지금 메타데이터로 만든 DSL 파일 수
 */
public record DeployCompareResult(
        String branch,
        String repositoryUrl,
        String baseCommitSha,
        int fileCount,
        List<DeployChange> changes,
        List<DslIssue> issues,
        boolean committable
) {
}
