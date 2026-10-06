package com.prometis.appbuilder.app.security.company;

import jakarta.persistence.*;
import lombok.*;

/**
 * 런타임 화면(pages/index)의 사이드바에서 선택하는 회사. 애플리케이션마다 따로 관리한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="COMPANY_UQ",columnNames={"applicationId","companyCode"})})
@Entity
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false, length = 100)
    private String companyCode;

    @Column(nullable = false, length = 200)
    private String companyName;

    // 런타임 사이드바와 콘솔 목록에 보이는 순서 (작을수록 앞, 비어 있으면 맨 뒤)
    @Column
    private Integer orderNum;

    public void update(String companyCode, String companyName, Integer orderNum) {
        this.companyCode = companyCode;
        this.companyName = companyName;
        this.orderNum = orderNum;
    }
}
