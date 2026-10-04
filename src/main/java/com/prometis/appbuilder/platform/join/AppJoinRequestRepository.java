package com.prometis.appbuilder.platform.join;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppJoinRequestRepository extends JpaRepository<AppJoinRequest, Long> {
    List<AppJoinRequest> findByApplicationIdOrderByRequestedAtAsc(String applicationId);

    Optional<AppJoinRequest> findByApplicationIdAndUserId(String applicationId, Long userId);
}
