package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.dto.ResponseMessage;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 에러메시지(ERROR_MESSAGE) 노드의 설정. 노드 하나에 한 행이다.
 * 흐름이 이 노드에 닿으면 실행을 멈추고 여기 적힌 status, code, message 로 에러응답을 돌려준다.
 * contents 는 응답에 함께 실어보낼 메시지ID 목록(쉼표 구분)이다. 앞 노드들이 채워둔 메시지 슬롯을 그대로 담는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class WorkflowErrorResponse {
    public static final int DEFAULT_STATUS = 400;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "WORKFLOW_ID", nullable = false)
    private Long workflowId;

    @Column(nullable = false, length = 100)
    private String nodeId;

    @Column
    private Integer status;

    @Column(length = 100)
    private String code;

    @Column(length = 2000)
    private String message;

    @Column(length = 1000)
    private String contents;

    public ResponseMessage toResponseMessage(Map<String, List<Map<String, Object>>> messageStorage) {
        return ResponseMessage.createErrorMessage(resolveStatus(status), code, message, resolveContents(messageStorage));
    }

    // 응답 HTTP 상태로 그대로 쓰이므로 에러 상태(4xx, 5xx)가 아니거나 알 수 없는 값이면 400 으로 보낸다.
    public static int resolveStatus(Integer status) {
        if(status == null) {
            return DEFAULT_STATUS;
        }

        HttpStatus httpStatus = HttpStatus.resolve(status);
        if(httpStatus == null || !httpStatus.isError()) {
            return DEFAULT_STATUS;
        }

        return status;
    }

    private Map<String, List<Map<String, Object>>> resolveContents(Map<String, List<Map<String, Object>>> messageStorage) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if(contents == null || contents.isBlank()) {
            return result;
        }

        for(String messageId : contents.split(",")) {
            String key = messageId.trim();
            if(key.isEmpty()) {
                continue;
            }

            result.put(key, messageStorage.getOrDefault(key, List.of()));
        }

        return result;
    }
}
