package com.prometis.appbuilder.app.security.permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByApplicationId(String applicationId);

    Optional<Permission> findByApplicationIdAndCode(String applicationId, String code);
}
