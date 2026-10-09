package com.prometis.appbuilder.app.image;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * 이미지 내용(content)을 뺀 메타정보. 목록 조회 때 이미지 바이트까지 읽지 않으려고 JPQL 생성자 표현식으로 만든다.
 *
 * @param url 앱에서 쓰는 이미지 링크 (ImageController: /{applicationId}/image/{storageType}/{filename})
 */
public record ImageInfo(
        Long id,
        String applicationId,
        String storageType,
        String filename,
        String originalFilename,
        String fileExtension,
        Long fileSize,
        String url
) {
    public ImageInfo(Long id, String applicationId, String storageType, String filename, String originalFilename, String fileExtension, Long fileSize) {
        this(id, applicationId, storageType, filename, originalFilename, fileExtension, fileSize, urlOf(applicationId, storageType, filename));
    }

    public static String urlOf(String applicationId, String storageType, String filename) {
        return "/" + UriUtils.encodePathSegment(applicationId, StandardCharsets.UTF_8)
                + "/image/" + UriUtils.encodePathSegment(storageType, StandardCharsets.UTF_8)
                + "/" + UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);
    }
}
