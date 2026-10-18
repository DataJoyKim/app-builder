package com.prometis.appbuilder.console.app.deploy.dto;

import com.prometis.appbuilder.console.app.deploy.dsl.DslFile;
import com.prometis.appbuilder.console.app.deploy.dsl.DslIssue;

import java.util.List;

/**
 * 지금 메타데이터로 만든 DSL 파일 전체와 그 검증 결과.
 * folders 는 DSL 폴더 전체(DslFolder 순서)라서, 화면이 파일이 없는 폴더도 보여줄 수 있다.
 */
public record DslSnapshotResponse(List<String> folders, List<DslFile> files, List<DslIssue> issues) {
}
