package com.prometis.appbuilder.file;

import com.prometis.appbuilder.expression.ParameterExpression;
import com.prometis.appbuilder.file.code.FileActionType;
import com.prometis.appbuilder.restapi.code.FileContentEncoding;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

/**
 * File 노드 정의. 워크플로우 노드의 기능코드(functionName)가 handlerName 이다.
 *
 * 실제 파일 위치 = 데이터소스(FileStorage)의 저장소경로 + filePath.
 * filePath 에는 #{파라미터} 표현식을 쓸 수 있다. 예) board/#{boardId}/#{fileName}
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="FILE_HANDLER_UQ",columnNames={"handlerName"})})
@Entity
public class FileHandler {
    public static final String FILE_CONTENT_KEY_DEFAULT = "fileContent";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String handlerName;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String dataSourceName;

    @Convert(converter = FileActionType.Converter.class)
    @Column(nullable = false, length = 100)
    private FileActionType actionType;

    @Column(nullable = false, length = 1000)
    private String filePath;

    // 업로드할 파일내용을 담은 파라미터 키이자, 조회결과에 파일내용을 담을 키. 비우면 fileContent.
    @Column(length = 100)
    private String fileContentKey;

    // 업로드 파일내용 해석방법.
    @Convert(converter = FileContentEncoding.Converter.class)
    @Column(length = 100)
    private FileContentEncoding fileContentEncoding;

    // 업로드 시 같은 경로에 파일이 있으면 덮어쓸지 여부.
    @Column
    private Boolean overwrite;

    public void update(
            String handlerName,
            String displayName,
            String dataSourceName,
            FileActionType actionType,
            String filePath,
            String fileContentKey,
            FileContentEncoding fileContentEncoding,
            Boolean overwrite
    ) {
        this.handlerName = handlerName;
        this.displayName = displayName;
        this.dataSourceName = dataSourceName;
        this.actionType = actionType;
        this.filePath = filePath;
        this.fileContentKey = fileContentKey;
        this.fileContentEncoding = fileContentEncoding;
        this.overwrite = overwrite;
    }

    public String resolveFilePath(ParameterExpression expression, Map<String, Object> params) {
        return expression.resolve(this.filePath, params);
    }

    public String resolveFileContentKey() {
        return (fileContentKey == null || fileContentKey.isBlank()) ? FILE_CONTENT_KEY_DEFAULT : fileContentKey;
    }

    public FileContentEncoding resolveFileContentEncoding() {
        return fileContentEncoding == null ? FileContentEncoding.BASE64 : fileContentEncoding;
    }

    public boolean shouldOverwrite() {
        return Boolean.TRUE.equals(overwrite);
    }
}
