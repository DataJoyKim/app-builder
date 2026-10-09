package com.prometis.appbuilder.app.image;

import jakarta.persistence.*;
import lombok.*;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="IMAGE_STORAGE_UQ",columnNames={"applicationId","storageType","filename"})})
@Entity
public class ImageStorage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(nullable = false, length = 100)
    private String storageType; // StorageType.class 의 storageType 값

    // 확장자 없는 링크 이름 (/{applicationId}/image/{storageType}/{filename}). 업로드 때 지정하지 않으면 원본 파일명에서 확장자를 뺀 이름
    @Column(nullable = false, length = 100)
    private String filename;

    // 실제 파일명 (확장자 포함)
    @Column(length = 100)
    private String originalFilename;

    @Column(length = 100)
    private String fileExtension;

    @Column
    private Long fileSize;

    @Lob
    @Column
    private byte[] content;
}
