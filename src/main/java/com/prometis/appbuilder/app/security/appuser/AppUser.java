package com.prometis.appbuilder.app.security.appuser;

import jakarta.persistence.*;
import lombok.*;

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
    private String authority;

    public void changeAuthority(String authority) {
        this.authority = authority;
    }
}
