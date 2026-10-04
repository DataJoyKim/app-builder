package com.prometis.appbuilder.app.security.usergroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGroupPermissionRepository extends JpaRepository<UserGroupPermission, Long> {
    List<UserGroupPermission> findByUserGroupId(Long userGroupId);

    List<UserGroupPermission> findByPermissionId(Long permissionId);

    boolean existsByUserGroupIdAndPermissionId(Long userGroupId, Long permissionId);
}
