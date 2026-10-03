package com.prometis.appbuilder.platform.application;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "application", uniqueConstraints = {@UniqueConstraint(name="APPLICATION_UQ",columnNames={"applicationId"})})
@Entity
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 서비스 경로의 prefix로 쓰이므로 생성 후에는 바꾸지 않는다
    @Column(nullable = false, length = 100, updatable = false)
    private String applicationId;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 100)
    private String status; // ACTIVE / INACTIVE

    @Column(length = 1000)
    private String description;

    public void update(
            String name,
            String status,
            String description
    ) {
        this.name = name;
        this.status = status;
        this.description = description;
    }
}
