package com.prometis.appbuilder.app.plugin;

import jakarta.persistence.*;
import lombok.*;

/**
 * 플러그인으로 들어오는 요청을 구분하는 라우팅 키. 플러그인 하나에 여러 개(1:N)를 둔다.
 *
 * <p>워크플로우 PLUGIN 노드는 기능코드(functionName)에 pluginPath 를 적고, 실행 시 이 행으로 어느 플러그인인지 찾은 뒤
 * 그 플러그인의 {@code WorkflowNodePlugin.execute(pluginPath, ...)} 를 부른다. 그래서 pluginPath 는 애플리케이션 안에서 유일하다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name = "PLUGIN_PATH_UQ", columnNames = {"applicationId", "pluginPath"})})
@Entity
public class PluginPath {
    // 워크플로우 노드 functionName 컬럼 길이(100)에 맞춘다. 영문/숫자로 시작하고 영문/숫자/_ . - / 만 쓴다
    public static final String PATH_PATTERN = "^[A-Za-z0-9][A-Za-z0-9_./-]{0,99}$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PLUGIN_SEQ", nullable = false)
    private Plugin plugin;

    @Column(nullable = false, length = 100)
    private String pluginPath;

    @Column(length = 200)
    private String displayName;

    @Column(length = 500)
    private String description;

    @Column
    private Integer orderNum;
}
