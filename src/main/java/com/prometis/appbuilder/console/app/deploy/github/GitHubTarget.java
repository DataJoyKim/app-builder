package com.prometis.appbuilder.console.app.deploy.github;

/**
 * GitHub 저장소 하나와 그 저장소에 쓸 토큰.
 */
public record GitHubTarget(String owner, String repository, String token) {

    @Override
    public String toString() {
        // 토큰이 로그에 찍히지 않게 한다
        return "GitHubTarget[" + owner + "/" + repository + "]";
    }
}
