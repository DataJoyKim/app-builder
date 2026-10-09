package com.prometis.appbuilder.app.image;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * application.yml의 platform.image 설정 (애플리케이션 콘솔 > Image Manage 업로드 제한).
 *
 * @param allowedExtensions 업로드할 수 있는 확장자 (대소문자, 앞의 점은 무시한다)
 * @param maxFileSize 파일 한 개의 최대 크기. spring.servlet.multipart 한도를 넘게 잡아도 그 한도에서 먼저 막힌다
 */
@ConfigurationProperties(prefix = "platform.image")
public record ImageProperties(List<String> allowedExtensions, DataSize maxFileSize) {
    private static final List<String> DEFAULT_ALLOWED_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "ico");
    private static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(5);

    public ImageProperties {
        if(allowedExtensions == null || allowedExtensions.isEmpty()) {
            allowedExtensions = DEFAULT_ALLOWED_EXTENSIONS;
        }
        allowedExtensions = allowedExtensions.stream()
                .filter(Objects::nonNull)
                .map(extension -> extension.trim().toLowerCase(Locale.ROOT))
                .map(extension -> extension.startsWith(".") ? extension.substring(1) : extension)
                .filter(extension -> !extension.isEmpty())
                .distinct()
                .toList();

        if(maxFileSize == null || maxFileSize.toBytes() <= 0) {
            maxFileSize = DEFAULT_MAX_FILE_SIZE;
        }
    }
}
