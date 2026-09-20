package com.prometis.appbuilder.security.ip;

import jakarta.persistence.*;
import lombok.*;

/**
 * IP 접근제어의 관리 단위. 허용할 IP 를 그룹으로 묶어두고, 워크플로우에 그룹을 매핑해서 쓴다.
 * 실제 IP 목록은 IpAddress 가 groupCode 가 아닌 id(ipGroupId)로 이 그룹을 가리킨다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="IP_GROUP_UQ",columnNames={"groupCode"})})
@Entity
public class IpGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String groupCode;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(length = 500)
    private String note;

    public void update(String groupCode, String displayName, String note) {
        this.groupCode = groupCode;
        this.displayName = displayName;
        this.note = note;
    }
}
