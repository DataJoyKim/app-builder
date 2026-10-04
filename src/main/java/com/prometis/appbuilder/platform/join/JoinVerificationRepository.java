package com.prometis.appbuilder.platform.join;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JoinVerificationRepository extends JpaRepository<JoinVerification, Long> {
    Optional<JoinVerification> findByVerificationKey(String verificationKey);

    List<JoinVerification> findByLoginId(String loginId);

    List<JoinVerification> findByEmailIgnoreCase(String email);

    List<JoinVerification> findByInvitationId(Long invitationId);
}
