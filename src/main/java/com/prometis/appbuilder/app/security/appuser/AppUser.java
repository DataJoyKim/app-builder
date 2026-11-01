package com.prometis.appbuilder.app.security.appuser;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="APP_USER_UQ",columnNames={"applicationId","userId"})})
@Entity
public class AppUser {
    public static final String AUTHORITY_APPLICATION_ADMIN = "APPLICATION_ADMIN";
    public static final String AUTHORITY_APPLICATION_USER = "APPLICATION_USER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column
    private Long userId;

    @Column(length = 100)
    private String companyCode;

    @Column(length = 100)
    private String authority;

    // 이 애플리케이션에 가입(AppUser 생성)한 시각. 이 값이 생기기 전에 가입한 사용자는 비어 있다
    @Column
    private LocalDateTime joinedAt;

    // 이 애플리케이션에 마지막으로 접속(인증)한 시각. 요청마다 쓰지 않도록 AppUserRepository.recordAccess 가 간격을 두고 갱신한다
    @Column
    private LocalDateTime lastAccessAt;

    @PrePersist
    void prePersist() {
        if(joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    public void changeAuthority(String authority) {
        this.authority = authority;
    }

    // 소속 회사 (Company.companyCode). 비어 있으면 회사 미지정
    public void changeCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public boolean hasCompany() {
        return companyCode != null && !companyCode.isBlank();
    }
}
