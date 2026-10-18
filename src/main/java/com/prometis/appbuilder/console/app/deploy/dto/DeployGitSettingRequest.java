package com.prometis.appbuilder.console.app.deploy.dto;

/**
 * 저장소 연동 저장 요청. token 이 비어 있으면 이미 저장된 토큰을 그대로 쓴다(저장소/브랜치만 바꿀 때).
 * repository 에는 저장소 이름 대신 owner/repository 나 https://github.com/owner/repository 를 넣어도 된다.
 */
public record DeployGitSettingRequest(String owner, String repository, String branch, String token) {
}
