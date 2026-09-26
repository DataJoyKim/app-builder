package com.prometis.appbuilder.app.security.domain;

import com.prometis.appbuilder.app.user.UserGroup;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class UserGroupAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Boolean lowerAuthorityGrant;

    @ManyToOne
    @JoinColumn(name = "AUTHORITY_CODE")
    private Authority authority;

    @ManyToOne
    @JoinColumn(name = "USER_GROUP_CODE")
    private UserGroup userGroup;

    public void update(UserGroup userGroup, Authority authority, Boolean lowerAuthorityGrant) {
        this.userGroup = userGroup;
        this.authority = authority;
        this.lowerAuthorityGrant = lowerAuthorityGrant;
    }
}
