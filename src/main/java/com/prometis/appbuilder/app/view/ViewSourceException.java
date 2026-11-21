package com.prometis.appbuilder.app.view;

/**
 * 화면 소스(FILE 유형 화면의 HTML 파일)를 읽고 쓸 수 없을 때. 메시지는 콘솔에 그대로 보여준다.
 */
public class ViewSourceException extends RuntimeException {
    public ViewSourceException(String message) {
        super(message);
    }

    public ViewSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
