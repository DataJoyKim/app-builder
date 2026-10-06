package com.prometis.appbuilder.platform.user;

import jakarta.persistence.*;
import lombok.*;

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
}
