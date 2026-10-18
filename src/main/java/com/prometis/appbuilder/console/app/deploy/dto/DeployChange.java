package com.prometis.appbuilder.console.app.deploy.dto;

/**
 * 저장소와 비교한 DSL 파일 하나의 변경. before 는 저장소의 내용, after 는 지금 메타데이터로 만든 내용이다.
 */
public record DeployChange(String path, String kind, String id, Status status, String before, String after) {
    public enum Status {
        ADDED, MODIFIED, DELETED
    }
}
