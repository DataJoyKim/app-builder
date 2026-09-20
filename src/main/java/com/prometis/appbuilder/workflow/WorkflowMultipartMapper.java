package com.prometis.appbuilder.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prometis.appbuilder.dto.RequestMessage;
import com.prometis.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * multipart/form-data 로 들어온 워크플로우 요청을 다룬다.
 *
 * 1. map() : 요청을 RequestMessage 로 만든다.
 *    - message : 워크플로우 요청메시지(JSON) 파트. header/body 는 JSON 요청과 똑같다. (필수)
 *    - files   : 업로드 파일 파트. 여러 개 보낼 수 있다.
 *    업로드 파일은 메시지(body)가 아니라 헤더(header.files)에 담는다.
 *    메시지에 담으면 실패 응답이나 에러메시지 노드를 타고 응답에 그대로 섞여나가기 때문이다.
 *
 * 2. toFileParams() : 파일 노드를 실행할 때 헤더의 업로드 파일을 노드 파라미터로 만들어준다.
 *    파일 하나가 한 행이고, 행에는 fileContent(MultipartFile) / fileName / contentType / fileSize 가 담긴다.
 *    노드의 요청메시지에 행이 있으면 공통값으로 합쳐준다.
 *    1행이면 모든 파일 행에 복사하고, 파일 수와 같으면 순서대로 짝지으며, 같은 키는 요청값을 그대로 둔다.
 *    예) 요청메시지 [{"boardId":"10"}] + 파일 2개 → boardId 가 두 파일 행에 모두 들어간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowMultipartMapper {
    public static final String MESSAGE_PART = "message";
    public static final String FILE_PART = "files";

    private final ObjectMapper objectMapper;

    public RequestMessage map(MultipartHttpServletRequest request) throws BusinessException {
        RequestMessage requestMessage = readMessage(request);

        validateFilePartNames(request);

        RequestMessage.Header header = requestMessage.getHeader();
        header.setFiles(request.getFiles(FILE_PART));

        return requestMessage;
    }

    private RequestMessage readMessage(MultipartHttpServletRequest request) throws BusinessException {
        String json = readMessagePart(request);

        if(json == null || json.isBlank()) {
            throw new BusinessException(WorkflowErrorMessage.NOT_FOUND_MULTIPART_MESSAGE);
        }

        try {
            RequestMessage requestMessage = objectMapper.readValue(json, RequestMessage.class);

            if(requestMessage == null || requestMessage.getHeader() == null) {
                throw new BusinessException(WorkflowErrorMessage.INVALID_MULTIPART_MESSAGE);
            }

            return requestMessage;
        }
        catch (IOException e) {
            log.error("Workflow multipart message parsing error.", e);
            throw new BusinessException(WorkflowErrorMessage.INVALID_MULTIPART_MESSAGE);
        }
    }

    // 폼 필드로 보내도 되고, Blob 파트로 보내도 받는다.
    private static String readMessagePart(MultipartHttpServletRequest request) throws BusinessException {
        String json = request.getParameter(MESSAGE_PART);
        if(json != null && !json.isBlank()) {
            return json;
        }

        MultipartFile part = request.getFile(MESSAGE_PART);
        if(part == null || part.isEmpty()) {
            return null;
        }

        try {
            return new String(part.getBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new BusinessException(WorkflowErrorMessage.INVALID_MULTIPART_MESSAGE);
        }
    }

    // 파일은 'files' 파트로만 받는다. 다른 이름으로 올린 파일은 조용히 버려지지않게 알려준다.
    private static void validateFilePartNames(MultipartHttpServletRequest request) throws BusinessException {
        for(String partName : request.getMultiFileMap().keySet()) {
            if(!FILE_PART.equals(partName) && !MESSAGE_PART.equals(partName)) {
                throw new BusinessException(WorkflowErrorMessage.INVALID_MULTIPART_FILE_PART);
            }
        }
    }
}
