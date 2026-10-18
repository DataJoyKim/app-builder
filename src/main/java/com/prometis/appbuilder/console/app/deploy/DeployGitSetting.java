package com.prometis.appbuilder.console.app.deploy;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 애플리케이션의 배포용 GitHub 저장소 연동 설정. 애플리케이션마다 하나다.
 * DSL 은 이 저장소의 branch 에 {applicationId}/ 폴더로 커밋된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name = "DEPLOY_GIT_SETTING_UQ", columnNames = {"APPLICATION_ID"})})
@Entity
public class DeployGitSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "APPLICATION_ID", nullable = false, length = 100)
    private String applicationId;

    // 저장소 소유자 (사용자 또는 조직)
    @Column(nullable = false, length = 100)
    private String owner;

    @Column(nullable = false, length = 100)
    private String repository;

    @Column(nullable = false, length = 200)
    private String branch;

    // Personal Access Token 암호문 (DeployTokenCipher). 화면에는 절대 내려보내지 않는다
    @Column(nullable = false, length = 2000)
    private String encryptedToken;

    // 연동할 때 토큰으로 확인한 GitHub 계정
    @Column(length = 100)
    private String githubLogin;

    @Column(length = 500)
    private String repositoryUrl;

    @Column
    private LocalDateTime updatedAt;

    public void update(String owner, String repository, String branch, String encryptedToken, String githubLogin, String repositoryUrl) {
        this.owner = owner;
        this.repository = repository;
        this.branch = branch;
        this.encryptedToken = encryptedToken;
        this.githubLogin = githubLogin;
        this.repositoryUrl = repositoryUrl;
        this.updatedAt = LocalDateTime.now();
    }
}
