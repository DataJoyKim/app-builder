package com.prometis.appbuilder.app.security.repository;

import com.prometis.appbuilder.app.security.domain.RefreshTokenStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenStoreRepository extends JpaRepository<RefreshTokenStore, Long> {
    Optional<RefreshTokenStore> findByUserId(Long userId);
}
