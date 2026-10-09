package com.prometis.appbuilder.app.image;

import jakarta.persistence.*;
import lombok.*;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="STORAGE_TYPE_UQ",columnNames={"applicationId","storageType"})})
@Entity
public class StorageType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false, length = 100)
    private String storageType;

    @Column(nullable = false, length = 200)
    private String displayName;

    // storageType 은 이미지 링크 경로라 바꾸지 않는다
    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
