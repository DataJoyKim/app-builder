package com.prometis.appbuilder.file;

import com.prometis.appbuilder.datasource.filestorage.DataSourceFileStorage;
import com.prometis.appbuilder.datasource.filestorage.DataSourceFileStorageRegister;
import com.prometis.appbuilder.executor.file.StorageType;
import com.prometis.appbuilder.file.code.FileActionType;
import com.prometis.appbuilder.file.code.FileResultCode;
import com.prometis.appbuilder.restapi.code.FileContentEncoding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * File 노드가 데이터소스 저장소경로 + 파일경로(#{} 표현식)로 파일을 업로드/조회/삭제하는지 확인한다.
 * 업로드 파일(MULTIPART)은 파라미터가 아니라 FileRequest 의 업로드 파일로 들어온다.
 */
class FileServiceTest {
    private static final String DATA_SOURCE = "LOCAL_FILES";

    @TempDir
    Path rootPath;

    private FileHandlerRepository repository;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        String options = "{\"rootPath\":\"" + rootPath.toString().replace("\\", "\\\\") + "\"}";

        DataSourceFileStorageRegister.initialize(List.of(DataSourceFileStorage.builder()
                .dataSourceName(DATA_SOURCE)
                .displayName("로컬 파일")
                .storageType(StorageType.LOCAL)
                .options(options)
                .build()));

        repository = mock(FileHandlerRepository.class);
        fileService = new FileService(repository);

        register("UPLOAD", FileActionType.UPLOAD, "board/#{boardId}/#{fileName}", null, false);
        register("UPLOAD_TEXT", FileActionType.UPLOAD, "board/#{boardId}/#{fileName}", FileContentEncoding.TEXT, false);
        register("UPLOAD_MULTIPART", FileActionType.UPLOAD, "board/#{boardId}/#{original_filename}", FileContentEncoding.MULTIPART, false);
        register("UPLOAD_MULTIPART_RENAME", FileActionType.UPLOAD, "board/#{boardId}/#{fileName}", FileContentEncoding.MULTIPART, true);
        register("READ", FileActionType.READ, "board/#{boardId}/#{fileName}", null, false);
        register("DELETE", FileActionType.DELETE, "board/#{boardId}/#{fileName}", null, false);
    }

    private void register(String handlerName, FileActionType actionType, String filePath, FileContentEncoding encoding, boolean overwrite) {
        FileHandler handler = FileHandler.builder()
                .handlerName(handlerName)
                .displayName(handlerName)
                .dataSourceName(DATA_SOURCE)
                .actionType(actionType)
                .filePath(filePath)
                .fileContentEncoding(encoding)
                .overwrite(overwrite)
                .build();

        when(repository.findByHandlerName(handlerName)).thenReturn(Optional.of(handler));
    }

    private static FileRequest request(Map<String, Object> params) {
        return FileRequest.builder().params(params).build();
    }

    private static FileRequest request(Map<String, Object> params, MultipartFile multipartFile) {
        return FileRequest.builder().params(params).multipartFile(multipartFile).build();
    }

    private static MultipartFile file(String originalFilename, byte[] content) {
        return new MockMultipartFile("files", originalFilename, "text/plain", content);
    }

    @Test
    void 업로드_조회_삭제() throws Exception {
        byte[] bytes = "hello file".getBytes(StandardCharsets.UTF_8);
        String base64 = "data:text/plain;base64," + Base64.getEncoder().encodeToString(bytes);

        FileResult upload = fileService.execute("UPLOAD", request(Map.of("boardId", "10", "fileName", "a.txt", "fileContent", base64)));

        assertEquals(FileResultCode.SUCCESS, upload.getResultCode(), upload.getMessage());
        assertEquals("board/10/a.txt", upload.getContent().get("filePath"));
        assertEquals(10L, upload.getContent().get("fileSize"));
        assertArrayEquals(bytes, Files.readAllBytes(rootPath.resolve("board/10/a.txt")));

        FileResult read = fileService.execute("READ", request(Map.of("boardId", "10", "fileName", "a.txt")));

        assertEquals(FileResultCode.SUCCESS, read.getResultCode(), read.getMessage());
        assertEquals("a.txt", read.getContent().get("fileName"));
        assertArrayEquals(bytes, (byte[]) read.getContent().get("fileContent"));

        FileResult delete = fileService.execute("DELETE", request(Map.of("boardId", "10", "fileName", "a.txt")));

        assertEquals(FileResultCode.SUCCESS, delete.getResultCode());
        assertEquals(true, delete.getContent().get("deleted"));
        assertFalse(Files.exists(rootPath.resolve("board/10/a.txt")));
    }

    @Test
    void 텍스트_인코딩과_byte배열_업로드() throws Exception {
        FileResult text = fileService.execute("UPLOAD_TEXT", request(Map.of("boardId", "1", "fileName", "t.txt", "fileContent", "한글")));
        assertEquals(FileResultCode.SUCCESS, text.getResultCode(), text.getMessage());
        assertEquals("한글", Files.readString(rootPath.resolve("board/1/t.txt"), StandardCharsets.UTF_8));

        byte[] bytes = {1, 2, 3};
        FileResult raw = fileService.execute("UPLOAD", request(Map.of("boardId", "1", "fileName", "b.bin", "fileContent", bytes)));
        assertEquals(FileResultCode.SUCCESS, raw.getResultCode(), raw.getMessage());
        assertArrayEquals(bytes, Files.readAllBytes(rootPath.resolve("board/1/b.bin")));
    }

    @Test
    void MULTIPART_업로드는_원본파일명으로_저장된다() throws Exception {
        byte[] bytes = "multipart content".getBytes(StandardCharsets.UTF_8);

        // 브라우저가 경로까지 보내도 파일명만 #{original_filename} 에 채워진다.
        FileResult result = fileService.execute("UPLOAD_MULTIPART",
                request(Map.of("boardId", "20"), file("c:\\temp\\m.txt", bytes)));

        assertEquals(FileResultCode.SUCCESS, result.getResultCode(), result.getMessage());
        assertEquals("board/20/m.txt", result.getContent().get("filePath"));
        assertEquals((long) bytes.length, result.getContent().get("fileSize"));
        assertArrayEquals(bytes, Files.readAllBytes(rootPath.resolve("board/20/m.txt")));
    }

    @Test
    void MULTIPART_업로드도_파일경로_파라미터로_이름을_바꿀수있다() throws Exception {
        byte[] bytes = "rename me".getBytes(StandardCharsets.UTF_8);

        FileResult renamed = fileService.execute("UPLOAD_MULTIPART_RENAME",
                request(Map.of("boardId", "20", "fileName", "rename.txt"), file("m.txt", bytes)));

        assertEquals(FileResultCode.SUCCESS, renamed.getResultCode(), renamed.getMessage());
        assertArrayEquals(bytes, Files.readAllBytes(rootPath.resolve("board/20/rename.txt")));

        // 덮어쓰기가 켜져있으면 같은 경로로 다시 올려도 성공한다.
        assertEquals(FileResultCode.SUCCESS, fileService.execute("UPLOAD_MULTIPART_RENAME",
                request(Map.of("boardId", "20", "fileName", "rename.txt"), file("m.txt", bytes))).getResultCode());
    }

    @Test
    void MULTIPART_인데_업로드_파일이_없으면_실패() {
        FileResult noFile = fileService.execute("UPLOAD_MULTIPART", request(Map.of("boardId", "21")));

        assertEquals(FileResultCode.FAILURE, noFile.getResultCode());
        assertTrue(noFile.getMessage().contains("업로드된 파일이 없습니다"), noFile.getMessage());
    }

    @Test
    void MULTIPART_인데_파일내용이_문자열로_오면_실패() {
        register("UPLOAD_MULTIPART_PARAM", FileActionType.UPLOAD, "board/#{boardId}/#{fileName}", FileContentEncoding.MULTIPART, false);

        // 업로드 파일 없이 파라미터로만 보낸 경우 (파일내용 문자열은 MULTIPART 정의에서 쓰지 않는다)
        FileResult result = fileService.execute("UPLOAD_MULTIPART_PARAM",
                request(Map.of("boardId", "21", "fileName", "t.txt", "fileContent", "YQ==")));

        assertEquals(FileResultCode.FAILURE, result.getResultCode());
        assertTrue(result.getMessage().contains("업로드된 파일이 없습니다"), result.getMessage());
    }

    @Test
    void 덮어쓰기가_꺼져있으면_같은파일_업로드는_실패() {
        Map<String, Object> params = Map.of("boardId", "1", "fileName", "dup.txt", "fileContent", "YQ==");

        assertEquals(FileResultCode.SUCCESS, fileService.execute("UPLOAD", request(params)).getResultCode());

        FileResult second = fileService.execute("UPLOAD", request(params));
        assertEquals(FileResultCode.FAILURE, second.getResultCode());
        assertTrue(second.getMessage().contains("이미 파일이 존재합니다"));

        byte[] bytes = "dup".getBytes(StandardCharsets.UTF_8);
        assertEquals(FileResultCode.SUCCESS, fileService.execute("UPLOAD_MULTIPART",
                request(Map.of("boardId", "1"), file("dup2.txt", bytes))).getResultCode());

        FileResult duplicated = fileService.execute("UPLOAD_MULTIPART",
                request(Map.of("boardId", "1"), file("dup2.txt", bytes)));
        assertEquals(FileResultCode.FAILURE, duplicated.getResultCode());
        assertTrue(duplicated.getMessage().contains("이미 파일이 존재합니다"));
    }

    @Test
    void 저장소경로_밖으로_나가는_경로는_거부() {
        FileResult result = fileService.execute("UPLOAD", request(Map.of("boardId", "../..", "fileName", "evil.txt", "fileContent", "YQ==")));

        assertEquals(FileResultCode.FAILURE, result.getResultCode());
        assertTrue(result.getMessage().contains("저장소경로 밖"));

        FileResult multipart = fileService.execute("UPLOAD_MULTIPART",
                request(Map.of("boardId", "../.."), file("evil.txt", "evil".getBytes(StandardCharsets.UTF_8))));

        assertEquals(FileResultCode.FAILURE, multipart.getResultCode());
        assertTrue(multipart.getMessage().contains("저장소경로 밖"));
    }

    @Test
    void 없는_파일_조회와_파일내용_누락은_실패() {
        FileResult read = fileService.execute("READ", request(Map.of("boardId", "1", "fileName", "none.txt")));
        assertEquals(FileResultCode.FAILURE, read.getResultCode());

        FileResult upload = fileService.execute("UPLOAD", request(Map.of("boardId", "1", "fileName", "x.txt")));
        assertEquals(FileResultCode.FAILURE, upload.getResultCode());
        assertTrue(upload.getMessage().contains("업로드할 파일내용이 없습니다"), upload.getMessage());
    }

    @Test
    void 정의나_데이터소스가_없으면_실패() {
        assertEquals(FileResultCode.FAILURE, fileService.execute("UNKNOWN", request(Map.of())).getResultCode());

        DataSourceFileStorageRegister.initialize(List.of());
        assertEquals(FileResultCode.FAILURE, fileService.execute("READ", request(Map.of("boardId", "1", "fileName", "a.txt"))).getResultCode());
    }
}
