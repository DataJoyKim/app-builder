package com.prometis.appbuilder.app.plugin;

import jakarta.persistence.*;
import lombok.*;

/**
 * 플러그인 jar 본문. 목록을 읽을 때 LOB 를 끌어오지 않도록 {@link Plugin} 과 나눴다.
 * 서버마다 이 본문을 내려받아 자기 플러그인 경로에 jar 로 쓴다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name = "PLUGIN_FILE_UQ", columnNames = {"pluginSeq"})})
@Entity
public class PluginFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plugin.id
    @Column(nullable = false)
    private Long pluginSeq;

    @Lob
    @Column(nullable = false)
    private byte[] content;

    public void replace(byte[] content) {
        this.content = content;
    }
}
