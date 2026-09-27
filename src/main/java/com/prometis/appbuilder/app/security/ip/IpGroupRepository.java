package com.prometis.appbuilder.app.security.ip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IpGroupRepository extends JpaRepository<IpGroup, Long> {
    Optional<IpGroup> findByApplicationIdAndGroupCode(String applicationId, String groupCode);

    List<IpGroup> findByApplicationIdAndGroupCodeIn(String applicationId, List<String> groupCodes);

    List<IpGroup> findByApplicationIdOrderByGroupCodeAsc(String applicationId);
}
