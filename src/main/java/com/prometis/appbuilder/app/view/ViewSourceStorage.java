package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.ViewObject;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * FILE 유형 화면의 HTML 소스를 서버 디스크에 둔다: {platform.view.root-path}/{applicationId}/views/{objectCode}.html
 * 소스는 런타임(/pages/{objectCode})에서 템플릿으로 해석하지 않고 글자 그대로 끼워 넣는다 (template/mf-template.html).
 */
@Component
@EnableConfigurationProperties(ViewProperties.class)
public class ViewSourceStorage {
    public static final String VIEWS_DIRECTORY = "views";
    public static final String SOURCE_EXTENSION = ".html";
    // 경로에 들어가므로 애플리케이션ID 도 디렉터리 이름으로 안전한 글자만 받는다
    private static final Pattern SAFE_APPLICATION_ID = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");

    private final Path rootPath;
    private final long maxFileSize;

    public ViewSourceStorage(ViewProperties properties) {
        this.rootPath = Paths.get(properties.rootPath()).toAbsolutePath().normalize();
        this.maxFileSize = properties.maxFileSize().toBytes();
    }

    public long maxFileSize() {
        return maxFileSize;
    }

    /**
     * 콘솔에 보여줄 저장 위치. 서버의 실제 디렉터리(root-path)는 드러내지 않는다
     */
    public static String displayPath(String applicationId, String objectCode) {
        return "{root-path}/" + applicationId + "/" + VIEWS_DIRECTORY + "/" + objectCode + SOURCE_EXTENSION;
    }

    public Optional<String> read(String applicationId, String objectCode) {
        Path file = fileOf(applicationId, objectCode);
        if(!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        try {
            return Optional.of(decode(Files.readAllBytes(file)));
        }
        catch (IOException e) {
            throw new ViewSourceException("화면 소스를 읽지 못했습니다. [" + objectCode + "]", e);
        }
    }

    public void write(String applicationId, String objectCode, String content) {
        byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        checkSize(bytes.length);

        Path file = fileOf(applicationId, objectCode);
        try {
            Files.createDirectories(file.getParent());
            // 쓰다가 실패해도 기존 파일이 반쯤 덮이지 않도록 임시 파일에 쓴 뒤 바꿔 끼운다
            Path temp = Files.createTempFile(file.getParent(), objectCode + ".", ".tmp");
            try {
                Files.write(temp, bytes);
                moveReplacing(temp, file);
            }
            finally {
                Files.deleteIfExists(temp);
            }
        }
        catch (IOException e) {
            throw new ViewSourceException("화면 소스를 저장하지 못했습니다. [" + objectCode + "]", e);
        }
    }

    /**
     * 업로드한 파일 내용을 UTF-8 글자로 바꿔 저장하고, 저장한 글자를 돌려준다
     */
    public String writeBytes(String applicationId, String objectCode, byte[] bytes) {
        checkSize(bytes.length);
        String content = decode(bytes);
        write(applicationId, objectCode, content);
        return content;
    }

    public void delete(String applicationId, String objectCode) {
        try {
            Files.deleteIfExists(fileOf(applicationId, objectCode));
        }
        catch (IOException e) {
            throw new ViewSourceException("화면 소스를 지우지 못했습니다. [" + objectCode + "]", e);
        }
    }

    private void checkSize(long size) {
        if(size > maxFileSize) {
            throw new ViewSourceException("화면 소스는 " + (maxFileSize / 1024) + "KB 까지 저장할 수 있습니다.");
        }
    }

    // {rootPath}/{applicationId}/views/{objectCode}.html. 이름 규칙에 맞지 않거나 views 밖을 가리키면 거부한다
    Path fileOf(String applicationId, String objectCode) {
        if(applicationId == null || !SAFE_APPLICATION_ID.matcher(applicationId).matches()) {
            throw new ViewSourceException("애플리케이션ID 가 올바르지 않습니다.");
        }
        if(!ViewObject.isValidCode(objectCode)) {
            throw new ViewSourceException("오브젝트코드는 " + ViewObject.CODE_RULE + " 이어야 합니다. [" + objectCode + "]");
        }

        Path directory = rootPath.resolve(applicationId).resolve(VIEWS_DIRECTORY).normalize();
        Path file = directory.resolve(objectCode + SOURCE_EXTENSION).normalize();
        if(!file.startsWith(directory) || !directory.startsWith(rootPath)) {
            throw new ViewSourceException("화면 소스 경로가 올바르지 않습니다. [" + objectCode + "]");
        }
        return file;
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // UTF-8 이 아닌 파일(EUC-KR 등)을 그대로 읽으면 글자가 깨진 채 저장되므로 거부한다. 앞의 BOM 은 뗀다
    private static String decode(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return text.startsWith("﻿") ? text.substring(1) : text;
        }
        catch (CharacterCodingException e) {
            throw new ViewSourceException("화면 소스는 UTF-8 로 저장된 파일이어야 합니다.");
        }
    }
}
