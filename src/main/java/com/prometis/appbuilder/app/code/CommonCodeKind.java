package com.prometis.appbuilder.app.code;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="COMMON_CODE_KIND_UQ",columnNames={"code"})})
@Entity
public class CommonCodeKind {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 300)
    private String name;

    public void update(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
