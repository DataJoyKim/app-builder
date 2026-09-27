package com.prometis.appbuilder.platform.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    Optional<User> findById(Long id);

    Optional<User> findByIdAndAuthority(Long id, String authority);
}
