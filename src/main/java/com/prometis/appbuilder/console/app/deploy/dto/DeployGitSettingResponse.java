package com.prometis.appbuilder.console.app.deploy.dto;

import com.prometis.appbuilder.console.app.deploy.DeployGitSetting;

import java.time.LocalDateTime;

/**
 * 저장소 연동 설정. 토큰은 내려보내지 않는다.
 */
public record DeployGitSettingResponse(
        boolean configured,
        String owner,
        String repository,
        String branch,
        String githubLogin,
        String repositoryUrl,
        LocalDateTime updatedAt
) {
    public static DeployGitSettingResponse notConfigured() {
        return new DeployGitSettingResponse(false, null, null, null, null, null, null);
    }

    public static DeployGitSettingResponse of(DeployGitSetting setting) {
        return new DeployGitSettingResponse(
                true,
                setting.getOwner(),
                setting.getRepository(),
                setting.getBranch(),
                setting.getGithubLogin(),
                setting.getRepositoryUrl(),
                setting.getUpdatedAt()
        );
    }
}
