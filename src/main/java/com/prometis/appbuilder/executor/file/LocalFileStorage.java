package com.prometis.appbuilder.executor.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.*;

/**
 * 서버 로컬 디스크 저장소. 모든 파일은 rootPath 아래에만 두며,
 * 파일경로에 '..' 등을 넣어 rootPath 밖으로 나가는 요청은 거부한다.
 */
@Slf4j
public class LocalFileStorage implements FileStorage {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final Path rootPath;

    public LocalFileStorage(String rootPath) {
        if(rootPath == null || rootPath.isBlank()) {
            throw new IllegalArgumentException("저장소경로(rootPath)가 지정되지않았습니다.");
        }

        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    public Path getRootPath() {
        return rootPath;
    }

    @Override
    public boolean exists(String filePath) throws FileStorageException {
        return Files.isRegularFile(resolve(filePath));
    }

    @Override
    public StoredFile read(String filePath) throws FileStorageException {
        Path target = resolve(filePath);

        if(!Files.isRegularFile(target)) {
            throw new FileStorageException("파일이 존재하지않습니다. [" + filePath + "]");
        }

        try {
            byte[] content = Files.readAllBytes(target);

            return toStoredFile(target, content.length, content);
        }
        catch (IOException e) {
            throw new FileStorageException("파일 조회에 실패하였습니다. [" + filePath + "]", e);
        }
    }

    @Override
    public StoredFile write(String filePath, byte[] content, boolean overwrite) throws FileStorageException {
        Path target = prepareTarget(filePath, overwrite);

        byte[] data = (content == null) ? new byte[0] : content;

        try {
            if(overwrite) {
                Files.write(target, data);
            }
            else {
                Files.write(target, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }

            return toStoredFile(target, data.length, null);
        }
        catch (FileAlreadyExistsException e) {
            throw new FileStorageException("이미 파일이 존재합니다. [" + filePath + "]", e);
        }
        catch (IOException e) {
            throw new FileStorageException("파일 업로드에 실패하였습니다. [" + filePath + "]", e);
        }
    }

    @Override
    public StoredFile write(String filePath, MultipartFile multipartFile, boolean overwrite) throws FileStorageException {
        try (InputStream content = multipartFile.getInputStream()) {
            Path target = prepareTarget(filePath, overwrite);

            // 스트림을 그대로 흘려보내므로 파일크기만큼 메모리를 쓰지않는다.
            long fileSize = overwrite
                    ? Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING)
                    : Files.copy(content, target);

            return toStoredFile(target, fileSize, null);
        }
        catch (FileAlreadyExistsException e) {
            throw new FileStorageException("이미 파일이 존재합니다. [" + filePath + "]", e);
        }
        catch (IOException e) {
            throw new FileStorageException("파일 업로드에 실패하였습니다. [" + filePath + "]", e);
        }
    }

    private Path prepareTarget(String filePath, boolean overwrite) throws FileStorageException {
        Path target = resolve(filePath);

        if(target.equals(rootPath)) {
            throw new FileStorageException("파일명이 지정되지않았습니다. [" + filePath + "]");
        }

        if(Files.isDirectory(target)) {
            throw new FileStorageException("같은 이름의 디렉토리가 존재합니다. [" + filePath + "]");
        }

        if(!overwrite && Files.exists(target)) {
            throw new FileStorageException("이미 파일이 존재합니다. [" + filePath + "]");
        }

        try {
            Files.createDirectories(target.getParent());
        }
        catch (IOException e) {
            throw new FileStorageException("파일 업로드에 실패하였습니다. [" + filePath + "]", e);
        }

        return target;
    }

    @Override
    public boolean delete(String filePath) throws FileStorageException {
        Path target = resolve(filePath);

        if(Files.isDirectory(target)) {
            throw new FileStorageException("디렉토리는 삭제할 수 없습니다. [" + filePath + "]");
        }

        try {
            return Files.deleteIfExists(target);
        }
        catch (IOException e) {
            throw new FileStorageException("파일 삭제에 실패하였습니다. [" + filePath + "]", e);
        }
    }

    @Override
    public void validate() throws FileStorageException {
        if(!Files.isDirectory(rootPath)) {
            throw new FileStorageException("저장소경로가 존재하지않거나 디렉토리가 아닙니다. [" + rootPath + "]");
        }

        if(!Files.isReadable(rootPath) || !Files.isWritable(rootPath)) {
            throw new FileStorageException("저장소경로에 읽기/쓰기 권한이 없습니다. [" + rootPath + "]");
        }
    }

    Path resolve(String filePath) throws FileStorageException {
        if(filePath == null || filePath.isBlank()) {
            throw new FileStorageException("파일경로가 지정되지않았습니다.");
        }

        // 파일경로는 항상 저장소경로 기준 상대경로로 본다. 앞의 '/', '\' 는 떼어낸다.
        String relative = filePath.trim().replace('\\', '/').replaceFirst("^/+", "");

        Path target;
        try {
            target = rootPath.resolve(relative).normalize();
        }
        catch (InvalidPathException e) {
            throw new FileStorageException("올바르지않은 파일경로입니다. [" + filePath + "]", e);
        }

        if(!target.startsWith(rootPath)) {
            throw new FileStorageException("저장소경로 밖의 파일에는 접근할 수 없습니다. [" + filePath + "]");
        }

        return target;
    }

    private StoredFile toStoredFile(Path target, long fileSize, byte[] content) {
        String fileName = target.getFileName().toString();

        return StoredFile.builder()
                .filePath(rootPath.relativize(target).toString().replace('\\', '/'))
                .fileName(fileName)
                .fileSize(fileSize)
                .contentType(guessContentType(target, fileName))
                .content(content)
                .build();
    }

    private static String guessContentType(Path target, String fileName) {
        try {
            String contentType = Files.probeContentType(target);
            if(contentType != null) {
                return contentType;
            }
        }
        catch (IOException e) {
            log.debug("probeContentType failed. [{}]", target, e);
        }

        String contentType = URLConnection.guessContentTypeFromName(fileName);

        return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
    }
}
