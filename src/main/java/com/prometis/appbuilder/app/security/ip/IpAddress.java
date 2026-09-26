package com.prometis.appbuilder.app.security.ip;

import jakarta.persistence.*;
import lombok.*;

/**
 * IP 그룹에 속한 허용 IP 한 건.
 * ipAddress 에는 단일 IP(192.168.0.10), 대역(192.168.0.0/24), 와일드카드(192.168.0.*), 전체(*) 를 쓸 수 있다.
 * IPv6 도 단일 IP(::1)와 대역(2001:db8::/32) 형태로 쓸 수 있다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class IpAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "IP_GROUP_ID", nullable = false)
    private Long ipGroupId;

    @Column(nullable = false, length = 100)
    private String ipAddress;

    @Column(length = 500)
    private String note;

    // 잠시 막아둘 때 쓰는 사용여부. 비우면 사용으로 본다.
    @Column
    private Boolean enabled;

    @Column
    private Integer orderNum;

    public boolean isEnabled() {
        return !Boolean.FALSE.equals(enabled);
    }
}
