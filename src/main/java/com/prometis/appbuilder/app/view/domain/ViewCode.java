package com.prometis.appbuilder.app.view.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="VIEW_CODE_UQ",columnNames={"objectCode","name"})})
@Entity
public class ViewCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String objectCode;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 100)
    private String target;
    @Column(length = 100)
    private String type;

    public void update(
            String objectCode,
            String name,
            String target,
            String type
    ) {
        this.objectCode = objectCode;
        this.name = name;
        this.target = target;
        this.type = type;
    }
}
