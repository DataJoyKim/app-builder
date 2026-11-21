package com.prometis.appbuilder.app.plugin;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 서버(인스턴스)마다 플러그인을 실제로 어떤 상태로 들고 있는지. 콘솔에서 서버별 반영 여부를 보여주는 용도이며 실행에는 쓰지 않는다.
 * 상태가 바뀔 때만 기록한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name = "PLUGIN_SERVER_STATE_UQ", columnNames = {"serverId", "applicationId", "pluginId"})})
@Entity
public class PluginServerState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String serverId;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false, length = 100)
    private String pluginId;

    // 이 서버가 로드한 jar 의 revision (로드 실패면 시도한 revision)
    @Column
    private Long revision;

    // pf4j PluginState (STARTED, STOPPED, FAILED ...)
    @Column(length = 30)
    private String state;

    @Column(length = 1000)
    private String message;

    @Column
    private LocalDateTime updatedAt;

    public void update(Long revision, String state, String message) {
        this.revision = revision;
        this.state = state;
        this.message = message;
        this.updatedAt = LocalDateTime.now();
    }
}
