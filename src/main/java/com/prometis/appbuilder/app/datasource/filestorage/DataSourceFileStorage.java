package com.prometis.appbuilder.app.datasource.filestorage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prometis.appbuilder.app.executor.file.FileStorage;
import com.prometis.appbuilder.app.executor.file.LocalFileStorage;
import com.prometis.appbuilder.app.executor.file.StorageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 파일저장소 데이터소스. 저장소유형(storageType)마다 설정이 달라서 설정값은 options(JSON)에 담는다.
 * LOCAL : {"rootPath": "C:/admin-builder/files"}
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Table(uniqueConstraints = {@UniqueConstraint(name="DATA_SOURCE_FILE_STORAGE_UQ",columnNames={"DATA_SOURCE_NAME"})})
@Entity
public class DataSourceFileStorage {
    public static final String OPTION_ROOT_PATH = "rootPath";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String dataSourceName;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String note;

    @Convert(converter = StorageType.Converter.class)
    @Column(length = 100)
    private StorageType storageType;

    @Lob
    @Column
    private String options;

    public FileStorage createDataSource() throws FileStorageCreationException {
        try {
            Map<String, String> optionsMap = (options == null || options.isBlank())
                    ? Map.of()
                    : new ObjectMapper().readValue(options, new TypeReference<>() {});

            if(this.storageType == StorageType.LOCAL) {
                return new LocalFileStorage(optionsMap.get(OPTION_ROOT_PATH));
            }
            else {
                throw new IllegalArgumentException("지원하지않는 저장소유형입니다. [" + this.storageType + "]");
            }
        }
        catch (Exception e) {
            throw new FileStorageCreationException(e);
        }
    }

    public void update(
            String dataSourceName,
            String displayName,
            String note,
            StorageType storageType,
            String options
    ) {
        this.dataSourceName = dataSourceName;
        this.displayName = displayName;
        this.note = note;
        this.storageType = storageType;
        this.options = options;
    }
}
