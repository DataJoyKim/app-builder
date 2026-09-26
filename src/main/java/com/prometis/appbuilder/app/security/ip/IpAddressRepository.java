package com.prometis.appbuilder.app.security.ip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpAddressRepository extends JpaRepository<IpAddress, Long> {
    List<IpAddress> findByIpGroupIdOrderByOrderNumAsc(Long ipGroupId);

    List<IpAddress> findByIpGroupIdIn(List<Long> ipGroupIds);

    void deleteByIpGroupId(Long ipGroupId);
}
