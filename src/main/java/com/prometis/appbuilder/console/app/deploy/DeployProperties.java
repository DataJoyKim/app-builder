package com.prometis.appbuilder.console.app.deploy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * application.yml 의 platform.deploy 설정 (애플리케이션 콘솔 > Deploy).
 *
 * @param githubApiUrl GitHub API 주소. GitHub Enterprise 면 https://{host}/api/v3
 * @param tokenSecretKey 저장소 토큰을 DB 에 암호화해서 넣을 때 쓰는 키. 운영에서는 반드시 환경변수로 바꿔야 한다.
 *                       바꾸면 이미 저장된 토큰은 풀 수 없으므로 연동 설정에서 토큰을 다시 넣어야 한다.
 * @param connectTimeout GitHub 연결 제한시간
 * @param inlineContentLimit 이 글자 수보다 큰 DSL 파일은 트리 요청에 본문을 싣지 않고 blob 으로 따로 올린다
 */
@ConfigurationProperties(prefix = "platform.deploy")
public record DeployProperties(String githubApiUrl, String tokenSecretKey, Duration connectTimeout, Integer inlineContentLimit) {
    private static final String DEFAULT_GITHUB_API_URL = "https://api.github.com";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final int DEFAULT_INLINE_CONTENT_LIMIT = 256 * 1024;

    public DeployProperties {
        if(githubApiUrl == null || githubApiUrl.isBlank()) {
            githubApiUrl = DEFAULT_GITHUB_API_URL;
        }
        githubApiUrl = githubApiUrl.replaceAll("/+$", "");

        if(connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }

        if(inlineContentLimit == null || inlineContentLimit <= 0) {
            inlineContentLimit = DEFAULT_INLINE_CONTENT_LIMIT;
        }
    }
}
