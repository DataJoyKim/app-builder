package com.prometis.appbuilder.app.node.executor;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.file.FileRequest;
import com.prometis.appbuilder.app.file.FileResult;
import com.prometis.appbuilder.app.file.FileService;
import com.prometis.appbuilder.app.node.NodeConfig;
import com.prometis.appbuilder.app.node.NodeResult;
import com.prometis.appbuilder.app.node.code.ResultType;
import com.prometis.appbuilder.app.node.executor.FileExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 파일 노드가 업로드 파일(헤더)과 요청메시지 행을 어떻게 짝지어 실행하는지 확인한다.
 */
class FileExecutorTest {
    private static final String HANDLER = "UPLOAD_BOARD_FILE";

    private FileService fileService;
    private FileExecutor fileExecutor;

    @BeforeEach
    void setUp() {
        fileService = mock(FileService.class);
        fileExecutor = new FileExecutor(fileService, new NodeConfig());

        when(fileService.execute(eq(HANDLER), any())).thenReturn(FileResult.success("업로드 성공하였습니다.", Map.of("filePath", "board/1/a.txt")));
    }

    private static RequestMessage.Header header(MultipartFile... files) {
        RequestMessage.Header header = new RequestMessage.Header();
        header.setWorkflowCode("WF01");
        header.setFiles(new ArrayList<>(List.of(files)));

        return header;
    }

    private static MultipartFile file(String fileName) {
        return new MockMultipartFile("files", fileName, "text/plain", fileName.getBytes(StandardCharsets.UTF_8));
    }

    private List<FileRequest> capturedRequests() {
        ArgumentCaptor<FileRequest> captor = ArgumentCaptor.forClass(FileRequest.class);
        verify(fileService, atLeastOnce()).execute(eq(HANDLER), captor.capture());

        return captor.getAllValues();
    }

    @Test
    void 파일마다_한번씩_실행하고_요청메시지_행과_순서대로_짝짓는다() {
        NodeResult result = fileExecutor.execute(
                null,
                HANDLER,
                header(file("a.txt"), file("b.txt")),
                List.of(Map.of("boardId", "1", "_seq", "1"), Map.of("boardId", "2", "_seq", "2"))
        );

        assertEquals(ResultType.SUCCESS, result.getResultType());
        assertEquals(2, result.getResults().size());

        List<FileRequest> requests = capturedRequests();
        assertEquals("a.txt", requests.get(0).getMultipartFile().getOriginalFilename());
        assertEquals("1", requests.get(0).getParams().get("boardId"));
        assertEquals("b.txt", requests.get(1).getMultipartFile().getOriginalFilename());
        assertEquals("2", requests.get(1).getParams().get("boardId"));
    }

    @Test
    void 요청메시지가_한행이면_모든_파일에_같은_값을_쓴다() {
        NodeResult result = fileExecutor.execute(
                null,
                HANDLER,
                header(file("a.txt"), file("b.txt")),
                List.of(Map.of("boardId", "7"))
        );

        assertEquals(ResultType.SUCCESS, result.getResultType());
        assertEquals(2, result.getResults().size());

        List<FileRequest> requests = capturedRequests();
        assertEquals("7", requests.get(0).getParams().get("boardId"));
        assertEquals("7", requests.get(1).getParams().get("boardId"));
        assertEquals("b.txt", requests.get(1).getMultipartFile().getOriginalFilename());
    }

    @Test
    void 요청메시지가_없어도_업로드_파일만으로_실행한다() {
        NodeResult result = fileExecutor.execute(null, HANDLER, header(file("a.txt")), List.of());

        assertEquals(ResultType.SUCCESS, result.getResultType());
        assertEquals(1, result.getResults().size());
        assertEquals("a.txt", capturedRequests().get(0).getMultipartFile().getOriginalFilename());
    }

    @Test
    void 행수가_파일수와_맞지않으면_실행하지않고_실패한다() {
        NodeResult result = fileExecutor.execute(
                null,
                HANDLER,
                header(file("a.txt"), file("b.txt")),
                List.of(Map.of("boardId", "1"), Map.of("boardId", "2"), Map.of("boardId", "3"))
        );

        assertEquals(ResultType.FAILURE, result.getResultType());
        assertTrue(String.valueOf(result.getResults().get(0).get("message")).contains("파일 수"));
        verify(fileService, never()).execute(any(), any());
    }

    @Test
    void 업로드_파일이_없으면_요청메시지_행마다_실행한다() {
        RequestMessage.Header header = new RequestMessage.Header();
        header.setWorkflowCode("WF01");

        NodeResult result = fileExecutor.execute(
                null,
                HANDLER,
                header,
                List.of(Map.of("boardId", "1", "_seq", "1"), Map.of("boardId", "2", "_seq", "2"))
        );

        assertEquals(ResultType.SUCCESS, result.getResultType());
        assertEquals(2, result.getResults().size());
        assertEquals("1", result.getResults().get(0).get("_seq"));
        assertNull(capturedRequests().get(0).getMultipartFile());
    }

    @Test
    void 한건이라도_실패하면_노드_결과가_실패다() {
        when(fileService.execute(eq(HANDLER), any()))
                .thenReturn(FileResult.success("업로드 성공하였습니다.", Map.of("filePath", "board/1/a.txt")))
                .thenReturn(FileResult.failure("이미 파일이 존재합니다."));

        NodeResult result = fileExecutor.execute(
                null,
                HANDLER,
                header(file("a.txt"), file("b.txt")),
                List.of(Map.of("boardId", "1"))
        );

        assertEquals(ResultType.FAILURE, result.getResultType());
        assertEquals("이미 파일이 존재합니다.", result.getResults().get(1).get("message"));
    }
}
