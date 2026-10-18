package com.prometis.appbuilder.console.app.deploy;

import lombok.Getter;

/**
 * 배포 화면에 그대로 보여줄 오류. status 는 응답 HTTP 상태다.
 */
@Getter
public class DeployException extends RuntimeException {
    private final int status;

    public DeployException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static DeployException badRequest(String message) {
        return new DeployException(400, message);
    }

    public static DeployException conflict(String message) {
        return new DeployException(409, message);
    }
}
