package com.prometis.appbuilder.app.security.appuser;

/**
 * 애플리케이션 사용자 권한 변경/삭제 요청이 규칙(허용 권한, 마지막 관리자 보호 등)에 맞지 않을 때. 메시지는 화면에 그대로 보여준다.
 */
public class AppUserManageException extends Exception {
    public AppUserManageException(String message) {
        super(message);
    }
}
