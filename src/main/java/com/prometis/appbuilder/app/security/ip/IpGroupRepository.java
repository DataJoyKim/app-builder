package com.prometis.appbuilder.app.security.ip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IpGroupRepository extends JpaRepository<IpGroup, Long> {
    Optional<IpGroup> findByGroupCode(String groupCode);

    List<IpGroup> findByGroupCodeIn(List<String> groupCodes);

    List<IpGroup> findAllByOrderByGroupCodeAsc();
}
