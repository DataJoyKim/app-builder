package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.dto.ResponseMessage;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
public class WorkflowController {
    @Autowired
    WorkflowService workflowService;
    @Autowired
    WorkflowMultipartMapper workflowMultipartMapper;

    @PostMapping("/workflow")
    public ResponseEntity<?> workflow(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @RequestBody RequestMessage requestMessage
    ) {
        ResponseMessage responseMessage = workflowService.execute(httpRequest, httpResponse, requestMessage);

        return new ResponseEntity<>(responseMessage, HttpStatus.valueOf(responseMessage.getStatus()));
    }

    /**
     * 파일 업로드용. 요청메시지(JSON)는 'message' 파트로, 업로드 파일은 'files' 파트로 받는다.
     * 파일은 'files' 메시지의 행(파일 하나가 한 행)으로 담기므로 FILE 노드가 그대로 받아 저장할 수 있다.
     */
    @PostMapping(value = "/workflow", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> workflowMultipart(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            MultipartHttpServletRequest multipartRequest
    ) {
        RequestMessage requestMessage;

        try {
            requestMessage = workflowMultipartMapper.map(multipartRequest);
        }
        catch (BusinessException e) {
            ResponseMessage errorMessage = ResponseMessage.createErrorMessage(e.getStatus(), e.getCode(), e.getMsg());

            return new ResponseEntity<>(errorMessage, HttpStatus.valueOf(errorMessage.getStatus()));
        }

        ResponseMessage responseMessage = workflowService.execute(httpRequest, httpResponse, requestMessage);

        return new ResponseEntity<>(responseMessage, HttpStatus.valueOf(responseMessage.getStatus()));
    }
}
