package com.prometis.appbuilder.security.repository;

import com.prometis.appbuilder.security.domain.RefreshTokenStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenStoreRepository extends JpaRepository<RefreshTokenStore, Long> {
    Optional<RefreshTokenStore> findByUserId(Long userId);
}
