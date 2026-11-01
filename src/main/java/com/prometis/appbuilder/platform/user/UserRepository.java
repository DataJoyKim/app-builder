package com.prometis.appbuilder.platform.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    Optional<User> findById(Long id);

    Optional<User> findByIdAndAuthority(Long id, String authority);

    // 로그인 시각만 바로 쓴다. 엔티티를 다시 읽어 저장하지 않으므로 다른 필드와 엇갈려 덮어쓸 일이 없다
    @Transactional
    @Modifying
    @Query("update User u set u.lastLoginAt = :loginAt where u.id = :userId")
    int recordLogin(@Param("userId") Long userId, @Param("loginAt") LocalDateTime loginAt);
}
