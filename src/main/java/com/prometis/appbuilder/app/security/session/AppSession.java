package com.prometis.appbuilder.app.security.session;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="APP_SESSION_UQ",columnNames={"applicationId","userId"})})
@Entity
public class AppSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column
    private Long userId;

    @Column(length = 100)
    private String companyCode;

    @Column
    private LocalDateTime createAt;

    public void changeCompany(String paramsCompanyCode) {
        this.companyCode = paramsCompanyCode;
        this.createAt = LocalDateTime.now();
    }
}
