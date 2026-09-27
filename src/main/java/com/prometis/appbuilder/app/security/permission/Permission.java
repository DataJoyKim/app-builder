package com.prometis.appbuilder.app.security.permission;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="PERMISSION_UQ",columnNames={"applicationId","code"})})
@Entity
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    public void update(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
