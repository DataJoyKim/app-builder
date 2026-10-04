package com.prometis.appbuilder.platform.join;

import jakarta.persistence.*;
import lombok.*;

/**
 * 애플리케이션별 가입 설정. 행이 없으면 초대만(INVITE_ONLY) 받는다.
 * defaultUserGroupId 는 앱 가입 페이지나 사용자 초대로 새로 들어온 사용자를 넣을 사용자 그룹이다 (없으면 그룹에 넣지 않는다).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="APP_JOIN_SETTING_UQ",columnNames={"applicationId"})})
@Entity
public class AppJoinSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false, length = 30)
    private String joinPolicy;

    @Column
    private Long defaultUserGroupId;

    public AppJoinPolicy policy() {
        AppJoinPolicy policy = AppJoinPolicy.of(joinPolicy);
        return policy == null ? AppJoinPolicy.INVITE_ONLY : policy;
    }

    public void update(AppJoinPolicy policy, Long defaultUserGroupId) {
        this.joinPolicy = policy.name();
        this.defaultUserGroupId = defaultUserGroupId;
    }
}
