package com.prometis.appbuilder.platform.user;

import com.prometis.core.crypto.PasswordEncoder;
import com.prometis.core.exception.BusinessException;
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

    public static User signUp(PasswordEncoder passwordEncoder, SignUpRequest request) throws BusinessException {
        if(!request.getPassword().equals(request.getCheckPassword())) {
            throw new BusinessException(UserErrorMessage.DIFFERENT_CHECK_PASSWORD);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        return User.builder()
                .loginId(request.getLoginId())
                .userName(request.getUserName())
                .password(encodedPassword)
                .email(request.getEmail())
                .build();
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
}
