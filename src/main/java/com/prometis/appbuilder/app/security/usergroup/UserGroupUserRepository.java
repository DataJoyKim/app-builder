package com.prometis.appbuilder.app.security.usergroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGroupUserRepository extends JpaRepository<UserGroupUser, Long> {
    List<UserGroupUser> findByUserId(Long userId);

    List<UserGroupUser> findByUserGroupId(Long userGroupId);

    // 한 애플리케이션의 사용자 그룹에 들어가 있는 소속만
    List<UserGroupUser> findByUserIdAndUserGroupApplicationId(Long userId, String applicationId);
}
