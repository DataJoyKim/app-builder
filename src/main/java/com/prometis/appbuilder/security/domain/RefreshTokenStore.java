package com.prometis.appbuilder.security.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name="REFRESH_TOKEN_STORE_UQ",columnNames={"USER_ID"})})
public class RefreshTokenStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String refreshToken;

    public static RefreshTokenStore createRefreshTokenStore(Long userId, String refreshToken) {
        return RefreshTokenStore.builder()
                .refreshToken(refreshToken)
                .userId(userId)
                .build();
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
