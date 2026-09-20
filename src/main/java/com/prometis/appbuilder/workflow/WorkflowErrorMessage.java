package com.prometis.appbuilder.workflow;

import com.prometis.core.exception.ErrorMessage;

public enum WorkflowErrorMessage implements ErrorMessage {
    NOT_SETTING_AUTHORITY(500, "E-WORKFLOW-001", "권한 설정이 되어있지않습니다. 관리자에게 문의해주세요."),
    NOT_HAS_AUTHORITIES(400, "E-WORKFLOW-002", "권한을 가지고있지않습니다."),
    PERMISSION_DENIED(400, "E-WORKFLOW-003", "접근권한이 존재하지않습니다."),
    NOT_FOUND_WORKFLOW(404, "E-WORKFLOW-004", "워크플로우가 존재하지않습니다."),
    FAILURE_CONDITION_EVALUATE(500, "E-WORKFLOW-005", "조건분기 판정식 실행에 실패하였습니다."),
    EXCEED_MAX_EXECUTE_STEP(500, "E-WORKFLOW-006", "실행 단계가 너무 많습니다. 노드 연결이 순환되고있는지 확인해주세요."),
    NOT_SETTING_ERROR_RESPONSE(500, "E-WORKFLOW-007", "에러메시지 노드의 설정이 되어있지않습니다. 관리자에게 문의해주세요."),
    NOT_FOUND_MULTIPART_MESSAGE(400, "E-WORKFLOW-008", "요청메시지(JSON)를 'message' 파트로 보내주세요."),
    INVALID_MULTIPART_MESSAGE(400, "E-WORKFLOW-009", "'message' 파트의 요청메시지 형식이 올바르지않습니다."),
    INVALID_MULTIPART_FILE_PART(400, "E-WORKFLOW-010", "업로드 파일은 'files' 파트로 보내주세요."),
    NOT_ALLOWED_IP(403, "E-WORKFLOW-011", "허용되지않은 IP 에서의 접근입니다."),
    ;
    private Integer status;
    private String errorCode;
    private String errorMsg;

    WorkflowErrorMessage(int status, String errorCode, String errorMsg) {
        this.status = status;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public Integer getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return errorCode;
    }

    @Override
    public String getMsg() {
        return errorMsg;
    }
}
