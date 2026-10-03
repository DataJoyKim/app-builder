package com.prometis.appbuilder.console.app;

/**
 * 애플리케이션 생성 요청이 규칙(소유 개수, ID 규칙, 중복 등)에 맞지 않을 때. 메시지는 화면에 그대로 보여준다.
 */
public class ApplicationManageException extends Exception {
    public ApplicationManageException(String message) {
        super(message);
    }
}
