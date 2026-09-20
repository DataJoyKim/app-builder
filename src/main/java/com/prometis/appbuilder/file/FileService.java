package com.prometis.appbuilder.file;

import com.prometis.appbuilder.datasource.LookupKey;
import com.prometis.appbuilder.datasource.filestorage.DataSourceFileStorageRegister;
import com.prometis.appbuilder.executor.file.FileStorage;
import com.prometis.appbuilder.executor.file.FileStorageException;
import com.prometis.appbuilder.executor.file.StoredFile;
import com.prometis.appbuilder.expression.ParameterExpression;
import com.prometis.appbuilder.file.code.FileActionType;
import com.prometis.appbuilder.restapi.code.FileContentEncoding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

/**
 * File 노드 실행. 정의(FileHandler)의 액션유형에 따라 파일을 조회/업로드/삭제한다.
 * 파일 위치 = 데이터소스 저장소경로 + 정의의 파일경로(#{파라미터} 표현식 적용).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    public static final String FILE_PATH_ORIGINAL_FILENAME = "original_filename";
    public static final String RESULT_FILE_PATH = "filePath";
    public static final String RESULT_FILE_NAME = "fileName";
    public static final String RESULT_FILE_SIZE = "fileSize";
    public static final String RESULT_CONTENT_TYPE = "contentType";
    public static final String RESULT_DELETED = "deleted";

    private final FileHandlerRepository fileHandlerRepository;

    public FileResult execute(String handlerName, FileRequest request) {
        Optional<FileHandler> handlerOptional = fileHandlerRepository.findByHandlerName(handlerName);

        if(handlerOptional.isEmpty()) {
            return FileResult.failure("요청한 리소스가 존재하지않습니다. [code:" + handlerName + "]");
        }

        FileHandler handler = handlerOptional.get();
        Map<String, Object> params = (request.getParams() == null) ? Map.of() : request.getParams();

        try {
            FileStorage fileStorage = DataSourceFileStorageRegister.getDataSource(LookupKey.generateKey(handler.getDataSourceName()));

            if(fileStorage == null) {
                throw new FileStorageException("등록되지않은 파일저장소 데이터소스입니다. [" + handler.getDataSourceName() + "]");
            }

            if(handler.getActionType() == null) {
                throw new FileStorageException("액션유형이 지정되지않았습니다. [code:" + handlerName + "]");
            }

            if(handler.getFilePath() == null || handler.getFilePath().isBlank()) {
                throw new FileStorageException("파일경로가 지정되지않았습니다. [code:" + handlerName + "]");
            }

            // 업로드된 파일이 있으면 원본 파일명을 파라미터로 채워 #{original_filename} 표현식에 쓸 수 있게 한다.
            params = withUploadedFileInfo(handler, params, request.getMultipartFile());

            String filePath = handler.resolveFilePath(new ParameterExpression(), params);

            return switch (handler.getActionType()) {
                case READ -> read(handler, fileStorage, filePath);
                case UPLOAD -> upload(handler, fileStorage, filePath, request.getMultipartFile(), params);
                case DELETE -> delete(fileStorage, filePath);
            };
        }
        catch (FileStorageException e) {
            log.error("File Execution Error. [code:{}]", handlerName, e);
            return FileResult.failure(e.getMessage());
        }
    }

    private FileResult read(FileHandler handler, FileStorage fileStorage, String filePath) throws FileStorageException {
        StoredFile file = fileStorage.read(filePath);

        Map<String, Object> content = toContent(file);
        content.put(handler.resolveFileContentKey(), file.getContent());

        return FileResult.success("조회 성공하였습니다.", content);
    }

    private FileResult upload(FileHandler handler, FileStorage fileStorage, String filePath, MultipartFile multipartFile, Map<String,Object> params) throws FileStorageException {
        StoredFile file;
        if(handler.resolveFileContentEncoding() == FileContentEncoding.MULTIPART) {
            if(multipartFile == null) {
                throw new FileStorageException("업로드된 파일이 없습니다. multipart/form-data 로 파일을 보내주세요.");
            }

            file = fileStorage.write(filePath, multipartFile, handler.shouldOverwrite());
        }
        else {
            String contentKey = handler.resolveFileContentKey();

            if(!params.containsKey(contentKey)) {
                throw new FileStorageException("업로드할 파일내용이 없습니다. [key:" + contentKey + "]");
            }

            Object rawContent = params.get(contentKey);

            file = fileStorage.write(filePath, toBytes(rawContent, handler), handler.shouldOverwrite());
        }

        return FileResult.success("업로드 성공하였습니다.", toContent(file));
    }

    private FileResult delete(FileStorage fileStorage, String filePath) throws FileStorageException {
        boolean deleted = fileStorage.delete(filePath);

        Map<String, Object> content = new HashMap<>();
        content.put(RESULT_FILE_PATH, filePath);
        content.put(RESULT_DELETED, deleted);

        return FileResult.success(deleted ? "삭제 성공하였습니다." : "삭제할 파일이 존재하지않습니다.", content);
    }

    /**
     * 업로드된 파일의 원본 파일명을 파라미터에 채워준다. (파일경로의 #{original_filename} 표현식용)
     * 요청에 같은 키의 값이 이미 있으면 요청값을 그대로 둔다.
     */
    private static Map<String, Object> withUploadedFileInfo(FileHandler handler, Map<String, Object> params, MultipartFile multipartFile) {
        if(handler.getActionType() != FileActionType.UPLOAD || multipartFile == null) {
            return params;
        }

        Map<String, Object> resolved = new HashMap<>(params);

        if(isBlank(resolved.get(FILE_PATH_ORIGINAL_FILENAME))) {
            resolved.put(FILE_PATH_ORIGINAL_FILENAME, toFileName(multipartFile.getOriginalFilename()));
        }

        return resolved;
    }

    private static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    // 브라우저가 경로까지 보내는 경우가 있어 파일명만 남긴다.
    public static String toFileName(String originalFilename) {
        if(originalFilename == null || originalFilename.isBlank()) {
            return "";
        }

        String fileName = originalFilename.trim();
        int separator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

        return (separator < 0) ? fileName : fileName.substring(separator + 1);
    }

    private static Map<String, Object> toContent(StoredFile file) {
        Map<String, Object> content = new HashMap<>();
        content.put(RESULT_FILE_PATH, file.getFilePath());
        content.put(RESULT_FILE_NAME, file.getFileName());
        content.put(RESULT_FILE_SIZE, file.getFileSize());
        content.put(RESULT_CONTENT_TYPE, file.getContentType());

        return content;
    }

    private static byte[] toBytes(Object rawContent, FileHandler handler) throws FileStorageException {
        if(rawContent == null) {
            return new byte[0];
        }

        if(rawContent instanceof byte[] bytes) {
            return bytes;
        }

        if(rawContent instanceof Blob blob) {
            try {
                return blob.getBytes(1, (int) blob.length());
            }
            catch (SQLException e) {
                throw new FileStorageException("파일내용을 읽을 수 없습니다.", e);
            }
        }

        if(rawContent instanceof String text) {
            switch (handler.resolveFileContentEncoding()) {
                case TEXT -> {
                    return text.getBytes(StandardCharsets.UTF_8);
                }
                case BASE64 -> {
                    try {
                        // data URL(data:image/png;base64,....) 로 온 값도 받는다.
                        int comma = text.startsWith("data:") ? text.indexOf(',') : -1;
                        return Base64.getMimeDecoder().decode(comma >= 0 ? text.substring(comma + 1) : text);
                    }
                    catch (IllegalArgumentException e) {
                        throw new FileStorageException("파일내용이 올바른 Base64 문자열이 아닙니다.", e);
                    }
                }
                case MULTIPART -> throw new FileStorageException("MULTIPART 인코딩은 multipart/form-data 로 업로드된 파일만 받습니다.");
            }
        }

        throw new FileStorageException("지원하지않는 파일내용 형식입니다.");
    }
}
