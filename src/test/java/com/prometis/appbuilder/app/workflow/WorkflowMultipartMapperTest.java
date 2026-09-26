package com.prometis.appbuilder.app.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.workflow.WorkflowMultipartMapper;
import com.prometis.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * multipart/form-data 워크플로우 요청이 'message'(JSON) + 'files'(업로드 파일) 로 읽히고,
 * 업로드 파일이 메시지(body)가 아니라 헤더에 담기는지 확인한다.
 */
class WorkflowMultipartMapperTest {
    private WorkflowMultipartMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WorkflowMultipartMapper(new ObjectMapper());
    }

    private static MockMultipartHttpServletRequest request(String message) {
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();

        if(message != null) {
            request.setParameter(WorkflowMultipartMapper.MESSAGE_PART, message);
        }

        return request;
    }

    private static MockMultipartFile file(String fileName, String content) {
        return new MockMultipartFile(
                WorkflowMultipartMapper.FILE_PART,
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    void 업로드파일은_메시지가_아니라_헤더에_담긴다() throws Exception {
        MockMultipartHttpServletRequest request = request(
                "{\"header\":{\"workflowCode\":\"WF01\"},\"body\":{\"uploadParam\":[{\"boardId\":\"7\"}]}}");
        request.addFile(file("a.txt", "aaa"));

        RequestMessage requestMessage = mapper.map(request);

        assertEquals("WF01", requestMessage.getHeader().getWorkflowCode());
        assertEquals(1, requestMessage.getHeader().getFiles().size());
        assertEquals("a.txt", requestMessage.getHeader().getFiles().get(0).getOriginalFilename());

        // 메시지는 JSON 요청과 똑같이 들어오고 파일은 섞이지 않는다.
        assertEquals(List.of("uploadParam"), List.copyOf(requestMessage.getBody().keySet()));
        assertEquals(Map.of("boardId", "7"), requestMessage.getBody().get("uploadParam").get(0));
    }

    @Test
    void 파일을_여러개_보내면_순서대로_헤더에_담긴다() throws Exception {
        MockMultipartHttpServletRequest request = request("{\"header\":{\"workflowCode\":\"WF01\"},\"body\":{}}");
        request.addFile(file("a.txt", "aaa"));
        request.addFile(file("b.txt", "bb"));

        RequestMessage.Header header = mapper.map(request).getHeader();

        assertEquals(2, header.getFiles().size());
        assertEquals("a.txt", header.getFiles().get(0).getOriginalFilename());
        assertEquals("b.txt", header.getFiles().get(1).getOriginalFilename());
    }

    @Test
    void 파일이_없으면_헤더의_업로드파일은_비어있다() throws Exception {
        RequestMessage requestMessage = mapper.map(request("{\"header\":{\"workflowCode\":\"WF01\"},\"body\":{}}"));

        assertTrue(requestMessage.getHeader().getFiles().isEmpty());
    }

    @Test
    void 요청메시지를_Blob_파트로_보내도_읽는다() throws Exception {
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.addFile(new MockMultipartFile(
                WorkflowMultipartMapper.MESSAGE_PART,
                "message.json",
                "application/json",
                "{\"header\":{\"workflowCode\":\"WF01\"},\"body\":{}}".getBytes(StandardCharsets.UTF_8)
        ));
        request.addFile(file("a.txt", "aaa"));

        RequestMessage requestMessage = mapper.map(request);

        assertEquals("WF01", requestMessage.getHeader().getWorkflowCode());
        assertEquals(1, requestMessage.getHeader().getFiles().size());
    }

    @Test
    void 요청메시지가_없거나_형식이_틀리면_실패() {
        assertEquals("E-WORKFLOW-008", codeOf(request(null)));
        assertEquals("E-WORKFLOW-009", codeOf(request("{invalid}")));
        assertEquals("E-WORKFLOW-009", codeOf(request("{\"body\":{}}")));
    }

    @Test
    void files가_아닌_파트로_올린_파일은_실패() {
        MockMultipartHttpServletRequest wrongPart = request("{\"header\":{\"workflowCode\":\"WF01\"},\"body\":{}}");
        wrongPart.addFile(new MockMultipartFile("attachment", "a.txt", "text/plain", "aaa".getBytes(StandardCharsets.UTF_8)));

        assertEquals("E-WORKFLOW-010", codeOf(wrongPart));
    }

    private String codeOf(MockMultipartHttpServletRequest request) {
        BusinessException e = assertThrows(BusinessException.class, () -> mapper.map(request));

        return e.getCode();
    }
}
