package com.prometis.appbuilder.platform.join;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 승인을 기다리는 애플리케이션 가입 신청 (가입 방식이 APPROVAL 일 때).
 * 관리자가 승인하면 사용자(APPLICATION_USER)로 등록하고 지운다. 거절해도 지운다 (다시 신청할 수 있다).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="APP_JOIN_REQUEST_UQ",columnNames={"applicationId","userId"})})
@Entity
public class AppJoinRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime requestedAt;
}
