package com.prometis.appbuilder.app.image;

/**
 * 콘솔 화면에 그대로 보여줄 이미지 관리 오류 (잘못된 입력, 확장자/크기 제한 위반 등).
 */
public class ImageException extends RuntimeException {
    public ImageException(String message) {
        super(message);
    }
}
