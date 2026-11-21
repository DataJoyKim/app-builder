package com.prometis.appbuilder.app.plugin;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 애플리케이션에 등록한 플러그인 (pf4j jar 하나).
 *
 * <p>서버가 여러 대여도 같은 플러그인을 쓰도록 DB 가 원본이다. jar 본문은 {@link PluginFile} 에 두고,
 * 각 서버는 {@link #revision} 이 바뀌면 DB 에서 jar 를 내려받아 자기 플러그인 경로({root}/{applicationId}/plugins)에 쓰고 다시 로드한다.
 * jar 를 새로 올리거나 리로드하면 revision 이 올라간다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name = "PLUGIN_UQ", columnNames = {"applicationId", "pluginId"})})
@Entity
public class Plugin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    // jar MANIFEST 의 Plugin-Id. 애플리케이션 안에서 유일하고, 같은 ID 의 jar 를 다시 올리면 새 버전으로 교체된다
    @Column(nullable = false, length = 100)
    private String pluginId;

    // MANIFEST 의 Plugin-Version
    @Column(length = 50)
    private String version;

    @Column(length = 100)
    private String provider;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String originalFilename;

    @Column
    private Long fileSize;

    // jar 본문의 SHA-256 (hex)
    @Column(length = 64)
    private String checksum;

    // jar 를 올리거나 리로드할 때마다 1 씩 오른다. 서버가 로드해 둔 revision 과 다르면 다시 로드한다
    @Column(nullable = false)
    private Long revision;

    @Convert(converter = PluginStatus.Converter.class)
    @Column(nullable = false, length = 20)
    private PluginStatus status;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if(createdAt == null) createdAt = now;
        if(updatedAt == null) updatedAt = now;
    }

    public void replaceJar(PluginJarInfo info, String originalFilename, long fileSize, String checksum) {
        this.version = info.version();
        this.provider = info.provider();
        this.description = info.description();
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.checksum = checksum;
        increaseRevision();
    }

    public void increaseRevision() {
        this.revision = (this.revision == null ? 0L : this.revision) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(PluginStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isStarted() {
        return PluginStatus.STARTED.equals(status);
    }
}
