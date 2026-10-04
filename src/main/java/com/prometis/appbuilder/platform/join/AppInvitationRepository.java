package com.prometis.appbuilder.platform.join;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppInvitationRepository extends JpaRepository<AppInvitation, Long> {
    Optional<AppInvitation> findByToken(String token);

    List<AppInvitation> findByApplicationIdOrderByCreatedAtDesc(String applicationId);

    List<AppInvitation> findByApplicationIdAndEmailIgnoreCaseAndAcceptedAtIsNull(String applicationId, String email);
}
