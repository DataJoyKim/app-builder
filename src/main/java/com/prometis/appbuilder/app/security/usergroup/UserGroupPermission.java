package com.prometis.appbuilder.app.security.usergroup;

import com.prometis.appbuilder.app.security.permission.Permission;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class UserGroupPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Boolean lowerPermissionGrant;

    @ManyToOne
    @JoinColumn(name = "PERMISSION_CODE")
    private Permission permission;

    @ManyToOne
    @JoinColumn(name = "USER_GROUP_CODE")
    private UserGroup userGroup;

    public void update(UserGroup userGroup, Permission permission, Boolean lowerPermissionGrant) {
        this.userGroup = userGroup;
        this.permission = permission;
        this.lowerPermissionGrant = lowerPermissionGrant;
    }
}
