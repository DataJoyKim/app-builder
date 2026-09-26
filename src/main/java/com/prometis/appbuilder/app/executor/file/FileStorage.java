package com.prometis.appbuilder.app.executor.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일저장소 공통 인터페이스. filePath 는 저장소경로(루트) 기준의 상대경로다.
 */
public interface FileStorage {
    boolean exists(String filePath) throws FileStorageException;

    StoredFile read(String filePath) throws FileStorageException;

    StoredFile write(String filePath, byte[] content, boolean overwrite) throws FileStorageException;

    StoredFile write(String filePath, MultipartFile multipartFile, boolean overwrite) throws FileStorageException;

    boolean delete(String filePath) throws FileStorageException;

    void validate() throws FileStorageException;
}
