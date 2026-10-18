package com.prometis.appbuilder.console.app.deploy.github;

import lombok.Getter;

/**
 * GitHub API 호출 실패. status 는 GitHub 가 돌려준 HTTP 상태(연결 자체가 안 되면 0)다.
 */
@Getter
public class GitHubException extends RuntimeException {
    private final int status;

    public GitHubException(int status, String message) {
        super(message);
        this.status = status;
    }

    public GitHubException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
