package com.prometis.appbuilder.app.view.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="VIEW_OBJECT_CONTENT_UQ",columnNames={"applicationId","objectCode"})})
@Entity
public class ViewObjectContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String applicationId;
    @Column(nullable = false, length = 100)
    private String objectCode;

    @Lob
    @Column
    private String content;

    public void update(String content) {
        this.content = content;
    }
}
