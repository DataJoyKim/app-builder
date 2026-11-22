package com.prometis.appbuilder.app.workflow.code;

/**
 * 워크플로우를 실행시킨 경로. 실행 로그(WorkflowExecutionLog)에 남긴다.
 * - WEB       : 화면 등에서 POST /{applicationId}/workflow 로 직접 호출 (기본값)
 * - REST_API  : Rest API(/{applicationId}/rest/**) 가 감싼 워크플로우
 * - SCHEDULER : 스케줄 작업
 */
public enum WorkflowTrigger {
    WEB, REST_API, SCHEDULER
}
