package com.prometis.appbuilder.platform.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(name="USERS_UQ",columnNames={"loginId"})})
@Entity
public class User {
    public static final String AUTHORITY_PLATFORM_ADMIN = "PLATFORM_ADMIN";
    // 애플리케이션을 만들고 소유할 수 있는 사용자 (공개 가입(/signup)으로 가입하면 받는다)
    public static final String AUTHORITY_APPLICATION_ADMIN = "APPLICATION_ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String userName;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 100)
    private String authority;

    // 계정을 만든 시각. 이 값이 생기기 전에 만든 계정은 비어 있다
    @Column
    private LocalDateTime createdAt;

    // 마지막으로 로그인(토큰 발급)한 시각. 비밀번호 로그인과 가입 직후 자동 로그인 모두 갱신한다
    @Column
    private LocalDateTime lastLoginAt;

    @PrePersist
    void prePersist() {
        if(createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void update(
            String loginId,
            String userName,
            String email,
            String authority
    ) {
        this.loginId = loginId;
        this.userName = userName;
        this.email = email;
        this.authority = authority;
    }

    /** 플랫폼 콘솔 사용자 화면에서 관리자가 비밀번호를 바꾼다. 해시된 값을 받는다 */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
