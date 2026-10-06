package com.prometis.appbuilder.app.security.usergroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    // 사용자 그룹 코드는 애플리케이션 + 회사 안에서만 유일하다
    Optional<UserGroup> findByApplicationIdAndCompanyCodeAndCode(String applicationId, String companyCode, String code);

    List<UserGroup> findByApplicationId(String applicationId);

    List<UserGroup> findByApplicationIdAndCompanyCode(String applicationId, String companyCode);

    boolean existsByApplicationIdAndCompanyCode(String applicationId, String companyCode);

    @Query("select g from UserGroup g left join fetch g.children where g.applicationId = :applicationId and g.parentUserGroup is null ORDER BY g.companyCode ASC, g.name ASC")
    List<UserGroup> findAllTree(@Param("applicationId") String applicationId);

    @Query("select g from UserGroup g left join fetch g.children where g.applicationId = :applicationId and g.companyCode = :companyCode and g.parentUserGroup is null ORDER BY g.name ASC")
    List<UserGroup> findAllTree(@Param("applicationId") String applicationId, @Param("companyCode") String companyCode);

    @Query("select g from UserGroup g left join fetch g.children where g.parentUserGroup = :parentUserGroup ORDER BY g.name ASC")
    List<UserGroup> findAllTree(@Param("parentUserGroup") UserGroup parentUserGroup);
}
