package com.prometis.appbuilder.app.security.usergroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    Optional<UserGroup> findByApplicationIdAndCode(String applicationId, String code);

    List<UserGroup> findByApplicationId(String applicationId);

    @Query("select g from UserGroup g left join fetch g.children where g.applicationId = :applicationId and g.parentUserGroup is null ORDER BY g.name ASC")
    List<UserGroup> findAllTree(@Param("applicationId") String applicationId);

    @Query("select g from UserGroup g left join fetch g.children where g.parentUserGroup = :parentUserGroup ORDER BY g.name ASC")
    List<UserGroup> findAllTree(@Param("parentUserGroup") UserGroup parentUserGroup);
}
