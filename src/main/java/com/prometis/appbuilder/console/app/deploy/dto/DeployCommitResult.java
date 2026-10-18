package com.prometis.appbuilder.console.app.deploy.dto;

public record DeployCommitResult(String commitSha, String commitUrl, String branch, int added, int modified, int deleted) {
}
