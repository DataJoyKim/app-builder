package com.prometis.appbuilder.console.app.deploy.dsl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * DSL 파일이 놓이는 폴더. 저장소에는 {applicationId}/{path}/{ID}.json 으로 저장된다.
 * 파일 하나가 메타데이터 하나(데이터소스, 워크플로우, 화면...)이고, 파일명은 그 메타데이터의 ID(idField 값)다.
 * 사이트(Layout)는 애플리케이션마다 하나뿐이라 ID 가 없어서 파일명을 FIXED_SITE_ID 로 고정한다.
 */
public enum DslFolder {
    DATABASE("datasource/database", "Database", "dataSourceName",
            List.of("displayName", "databaseKind", "url", "username")),
    REST_SERVER("datasource/rest-server", "RestServer", "dataSourceName",
            List.of("displayName", "baseUrl")),
    NOTIFICATION_PROVIDER("datasource/notification-provider", "NotificationProvider", "dataSourceName",
            List.of("displayName", "type")),
    WORKFLOW("workflow/workflow", "Workflow", "workflowCode",
            List.of("displayName", "useAuthValidation")),
    QUERY("workflow/query", "Query", "queryName",
            List.of("displayName", "dataSourceName")),
    ENTITY("workflow/entity", "Entity", "entityName",
            List.of("displayName", "dataSourceName", "tableName")),
    REST_CLIENT("workflow/rest-client", "RestClient", "clientName",
            List.of("displayName", "dataSourceName", "method")),
    NOTIFICATION("workflow/notification", "Notification", "notificationName",
            List.of("displayName", "dataSourceName")),
    MESSAGE_PROCESSOR("workflow/message", "MessageProcessor", "processorName",
            List.of("displayName")),
    REST_API("rest-api", "RestApi", "apiCode",
            List.of("displayName", "httpMethod", "path", "workflowCode", "enabled")),
    SCHEDULER("scheduler", "SchedulerJob", "jobCode",
            List.of("jobName", "cronExpression", "enabled")),
    VIEW_OBJECT("view/view-object", "ViewObject", "objectCode",
            List.of("objectName")),
    MENU("view/menu", "Menu", "menuCd",
            List.of("menuNm")),
    SITE("view/site", "Site", null,
            List.of()),
    COMPANY("security/company", "Company", "companyCode",
            List.of("companyName")),
    IMAGE_STORAGE_TYPE("setting/image-manage", "ImageStorageType", "storageType",
            List.of("displayName")),
    COMMON_CODE("setting/common-code", "CommonCodeKind", "code",
            List.of("name"));

    public static final String FIXED_SITE_ID = "site";
    public static final String FILE_EXTENSION = ".json";

    private final String path;
    private final String kind;
    private final String idField;
    private final List<String> requiredFields;

    DslFolder(String path, String kind, String idField, List<String> requiredFields) {
        this.path = path;
        this.kind = kind;
        this.idField = idField;
        this.requiredFields = requiredFields;
    }

    public String path() {
        return path;
    }

    public String kind() {
        return kind;
    }

    // ID 가 담긴 필드. 사이트처럼 ID 가 없는 폴더는 null
    public String idField() {
        return idField;
    }

    public List<String> requiredFields() {
        return requiredFields;
    }

    public String filePath(String applicationId, String id) {
        return applicationId + "/" + path + "/" + id + FILE_EXTENSION;
    }

    public static Optional<DslFolder> ofPath(String path) {
        return Arrays.stream(values())
                .filter(folder -> folder.path.equals(path))
                .findFirst();
    }
}
