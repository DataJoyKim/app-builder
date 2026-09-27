package com.prometis.appbuilder.app.security.appuser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    List<AppUser> findByApplicationId(String applicationId);

    List<AppUser> findByApplicationIdAndAuthority(String applicationId, String authority);

    Optional<AppUser> findByApplicationIdAndUserId(String applicationId, Long userId);

    Optional<AppUser> findByUserIdAndApplicationIdAndAuthority(Long userId, String applicationId, String authority);
}
