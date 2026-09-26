package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.datasource.filestorage.DataSourceFileStorage;
import com.prometis.appbuilder.app.datasource.filestorage.DataSourceFileStorageRegister;
import com.prometis.appbuilder.app.executor.file.StorageType;
import com.prometis.appbuilder.app.file.FileHandler;
import com.prometis.appbuilder.app.file.FileHandlerRepository;
import com.prometis.appbuilder.app.file.code.FileActionType;
import com.prometis.appbuilder.app.node.WorkflowNode;
import com.prometis.appbuilder.app.node.WorkflowNodeRepository;
import com.prometis.appbuilder.app.node.code.FunctionType;
import com.prometis.appbuilder.app.restapi.code.FileContentEncoding;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * multipart/form-data 로 올린 파일이 /workflow → 헤더 → FILE 노드 → 파일저장소까지 실제로 저장되는지 확인한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow-multipart;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc
class WorkflowMultipartUploadTest {
    private static final String DATA_SOURCE = "LOCAL_FILES";
    private static final String WORKFLOW_CODE = "WF_FILE_UPLOAD";
    private static final String HANDLER_NAME = "UPLOAD_BOARD_FILE";

    @TempDir
    Path rootPath;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    WorkflowRepository workflowRepository;
    @Autowired
    WorkflowNodeRepository workflowNodeRepository;
    @Autowired
    FileHandlerRepository fileHandlerRepository;

    @BeforeEach
    void setUp() {
        // 테스트마다 같은 정의를 다시 넣으므로 먼저 비운다. (H2 인메모리 DB 를 테스트끼리 공유한다)
        workflowNodeRepository.deleteAll();
        workflowRepository.deleteAll();
        fileHandlerRepository.deleteAll();

        DataSourceFileStorageRegister.initialize(List.of(DataSourceFileStorage.builder()
                .dataSourceName(DATA_SOURCE)
                .displayName("로컬 파일")
                .storageType(StorageType.LOCAL)
                .options("{\"rootPath\":\"" + rootPath.toString().replace("\\", "\\\\") + "\"}")
                .build()));

        fileHandlerRepository.save(FileHandler.builder()
                .handlerName(HANDLER_NAME)
                .displayName("게시판 첨부 업로드")
                .dataSourceName(DATA_SOURCE)
                .actionType(FileActionType.UPLOAD)
                // 업로드된 파일의 원본 파일명으로 저장한다.
                .filePath("board/#{boardId}/#{original_filename}")
                .fileContentEncoding(FileContentEncoding.MULTIPART)
                .overwrite(true)
                .build());

        Workflow workflow = workflowRepository.save(Workflow.builder()
                .workflowCode(WORKFLOW_CODE)
                .displayName("첨부파일 업로드")
                .useAuthValidation(false)
                .build());

        workflowNodeRepository.save(WorkflowNode.builder()
                .workflowId(workflow.getId())
                .nodeId("node-1")
                .functionName(HANDLER_NAME)
                .functionType(FunctionType.FILE)
                .orderNum(1)
                .isLogging(false)
                .requestMessageId("uploadParam")
                .responseMessageId("uploadResult")
                .build());
    }

    @Test
    void files_파트로_올린_파일이_FILE노드로_저장된다() throws Exception {
        byte[] content = "첨부파일 내용".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("files", "첨부.txt", "text/plain", content);

        MvcResult result = mockMvc.perform(multipart("/workflow")
                        .file(file)
                        // 파일경로에 쓸 값은 파일 노드의 요청메시지로 보낸다.
                        .param("message", "{\"header\":{\"workflowCode\":\"" + WORKFLOW_CODE + "\"},\"body\":{\"uploadParam\":[{\"boardId\":\"7\"}]}}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("\"resultType\":\"SUCCESS\""), body);
        assertTrue(body.contains("board/7/첨부.txt"), body);
        // 업로드 파일은 헤더로만 들고다니므로 응답에는 파일 객체가 실리지 않는다.
        assertFalse(body.contains("fileContent"), body);

        assertArrayEquals(content, Files.readAllBytes(rootPath.resolve("board/7/첨부.txt")));
    }

    @Test
    void 파일을_여러개_올리면_공통_요청메시지_한행으로_모두_저장된다() throws Exception {
        MockMultipartFile first = new MockMultipartFile("files", "first.txt", "text/plain", "1번".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile second = new MockMultipartFile("files", "second.txt", "text/plain", "2번".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/workflow")
                        .file(first)
                        .file(second)
                        .param("message", "{\"header\":{\"workflowCode\":\"" + WORKFLOW_CODE + "\"},\"body\":{\"uploadParam\":[{\"boardId\":\"9\"}]}}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("\"resultType\":\"SUCCESS\""), body);
        assertEquals("1번", Files.readString(rootPath.resolve("board/9/first.txt"), StandardCharsets.UTF_8));
        assertEquals("2번", Files.readString(rootPath.resolve("board/9/second.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void 파일수와_요청메시지_행수가_맞지않으면_실패로_응답한다() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/workflow")
                        .file(new MockMultipartFile("files", "a.txt", "text/plain", "a".getBytes(StandardCharsets.UTF_8)))
                        .param("message", "{\"header\":{\"workflowCode\":\"" + WORKFLOW_CODE + "\"},\"body\":{\"uploadParam\":[{\"boardId\":\"1\"},{\"boardId\":\"2\"}]}}"))
                .andExpect(status().is5xxServerError())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("요청메시지 행 수가 맞지 않습니다"), body);
    }

    @Test
    void 파일없이_호출하면_업로드_실패로_응답한다() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/workflow")
                        .param("message", "{\"header\":{\"workflowCode\":\"" + WORKFLOW_CODE + "\"},\"body\":{\"uploadParam\":[{\"boardId\":\"7\"}]}}"))
                .andExpect(status().is5xxServerError())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("업로드된 파일이 없습니다"), body);
    }

    @Test
    void 요청메시지_파트가_없으면_400() throws Exception {
        mockMvc.perform(multipart("/workflow")
                        .file(new MockMultipartFile("files", "a.txt", "text/plain", "aaa".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest());
    }
}
