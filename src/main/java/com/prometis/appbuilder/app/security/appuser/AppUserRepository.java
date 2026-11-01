package com.prometis.appbuilder.app.security.appuser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    List<AppUser> findByApplicationId(String applicationId);

    List<AppUser> findByApplicationIdAndAuthority(String applicationId, String authority);

    Optional<AppUser> findByApplicationIdAndUserId(String applicationId, Long userId);

    List<AppUser> findByUserId(Long userId);

    List<AppUser> findByUserIdAndAuthority(Long userId, String authority);

    Optional<AppUser> findByUserIdAndApplicationIdAndAuthority(Long userId, String applicationId, String authority);

    /**
     * 마지막 접속 시각을 갱신한다. 인증은 요청마다 일어나므로, 이미 refreshBefore 이후에 기록돼 있으면 건너뛴다
     * (조건이 쿼리 안에 있어 읽지 않고 한 번에 처리된다). 이 애플리케이션 사용자가 아니면 아무것도 바뀌지 않는다.
     */
    @Transactional
    @Modifying
    @Query("update AppUser u set u.lastAccessAt = :accessAt"
            + " where u.applicationId = :applicationId and u.userId = :userId"
            + " and (u.lastAccessAt is null or u.lastAccessAt < :refreshBefore)")
    int recordAccess(@Param("applicationId") String applicationId, @Param("userId") Long userId,
                     @Param("accessAt") LocalDateTime accessAt, @Param("refreshBefore") LocalDateTime refreshBefore);
}
