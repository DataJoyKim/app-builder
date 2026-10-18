package com.prometis.appbuilder.console.app.deploy.dto;

/**
 * @param baseCommitSha 비교(compare) 결과로 받은 커밋. 그 뒤 브랜치에 다른 커밋이 올라왔으면 커밋하지 않는다
 */
public record DeployCommitRequest(String message, String baseCommitSha) {
}
