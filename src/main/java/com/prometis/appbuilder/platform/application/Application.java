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

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 100)
    private String status; // ACTIVE / INACTIVE

    @Column(length = 1000)
    private String description;

    public void update(
            String applicationId,
            String name,
            String status,
            String description
    ) {
        this.applicationId = applicationId;
        this.name = name;
        this.status = status;
        this.description = description;
    }
}
