package com.prometis.appbuilder.console.app.deploy.dto;

import java.util.List;

/**
 * 가져오면 바뀌는 메타데이터 하나 (종류 + ID).
 *
 * @param action CREATE(저장소에만 있음) / UPDATE(내용이 다름) / DELETE(이 환경에만 있음)
 * @param files  그 메타데이터의 파일별 변경. before 는 이 환경의 내용, after 는 저장소의 내용
 */
public record DeployImportChange(String kind, String id, String action, List<FileChange> files) {

    /**
     * @param path   애플리케이션 폴더 아래 경로 (workflow/query/Q001/query.sql)
     * @param status ADDED / MODIFIED / DELETED (이 환경 기준으로 가져오면 생기는/바뀌는/없어지는 파일)
     */
    public record FileChange(String path, String status, String before, String after) {
    }
}
