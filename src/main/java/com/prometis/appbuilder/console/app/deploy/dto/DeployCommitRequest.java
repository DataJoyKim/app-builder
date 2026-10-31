package com.prometis.appbuilder.console.app.deploy.dto;

import java.util.List;

/**
 * @param baseCommitSha 비교(compare) 결과로 받은 커밋. 그 뒤 브랜치에 다른 커밋이 올라왔으면 커밋하지 않는다
 * @param paths 커밋할 파일 경로(비교 결과의 DeployChange.path). null 이면 바뀐 파일 전부.
 *              메타데이터 하나에 딸린 파일(metadata.json 과 query.sql/script.js, 화면 액션...)은 하나만 골라도 함께 커밋된다
 */
public record DeployCommitRequest(String message, String baseCommitSha, List<String> paths) {
}
