package com.prometis.appbuilder.app.user;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class UserGroupUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private User user;

    @ManyToOne
    @JoinColumn(name = "USER_GROUP_CODE")
    private UserGroup userGroup;

    public void update(UserGroup userGroup, User user) {
        this.userGroup = userGroup;
        this.user = user;
    }
}
