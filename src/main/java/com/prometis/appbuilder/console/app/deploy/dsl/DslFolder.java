package com.prometis.appbuilder.console.app.deploy.dsl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * DSL 파일이 놓이는 폴더. 저장소에는 {applicationId}/{path}/{ID}.json 으로 저장된다.
 * 파일 하나가 메타데이터 하나(데이터소스, 워크플로우, 화면...)이고, 파일명은 그 메타데이터의 ID(idField 값)다.
 * 사이트(Layout)는 애플리케이션마다 하나뿐이라 ID 가 없어서 파일명을 FIXED_SITE_ID 로 고정한다.
 *
 * SQL, 스크립트처럼 JSON 에 넣으면 읽기 어려운 본문이 있는 메타데이터는 메타데이터마다 {ID}/ 폴더를 하나씩 두고(폴더명이 ID),
 * 본문을 따로 떼어낸다. 그 폴더 안에 둘 수 있는 파일이 parts 다.
 *   쿼리(QUERY)
 *     {applicationId}/workflow/query/{queryName}/metadata.json : 쿼리 설정(데이터소스, 파라미터...)
 *     {applicationId}/workflow/query/{queryName}/query.sql     : SQL 본문
 *   메시지 프로세서(MESSAGE_PROCESSOR)
 *     {applicationId}/workflow/message/{processorName}/metadata.json : 프로세서 설정
 *     {applicationId}/workflow/message/{processorName}/script.js     : 변환 스크립트 본문
 *   화면(VIEW_OBJECT)
 *     {applicationId}/view/view-object/{objectCode}/metadata.json                      : 화면 설정
 *     {applicationId}/view/view-object/{objectCode}/actions/{actionName}/metadata.json : 화면 액션 설정
 *     {applicationId}/view/view-object/{objectCode}/actions/{actionName}/script.js     : 액션 스크립트 (스크립트가 있을 때만)
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
            List.of("displayName", "dataSourceName"), Set.of(Part.SQL)),
    ENTITY("workflow/entity", "Entity", "entityName",
            List.of("displayName", "dataSourceName", "tableName")),
    REST_CLIENT("workflow/rest-client", "RestClient", "clientName",
            List.of("displayName", "dataSourceName", "method")),
    NOTIFICATION("workflow/notification", "Notification", "notificationName",
            List.of("displayName", "dataSourceName")),
    MESSAGE_PROCESSOR("workflow/message", "MessageProcessor", "processorName",
            List.of("displayName"), Set.of(Part.SCRIPT)),
    REST_API("rest-api", "RestApi", "apiCode",
            List.of("displayName", "httpMethod", "path", "workflowCode", "enabled")),
    SCHEDULER("scheduler", "SchedulerJob", "jobCode",
            List.of("jobName", "cronExpression", "enabled")),
    VIEW_OBJECT("view/view-object", "ViewObject", "objectCode",
            List.of("objectName"), Set.of(Part.ACTION, Part.ACTION_SCRIPT)),
    MENU("view/menu", "Menu", "menuCd",
            List.of("menuNm")),
    SITE("view/site", "Site", null,
            List.of()),
    COMMON_CODE("setting/common-code", "CommonCodeKind", "code",
            List.of("name"));

    public static final String FIXED_SITE_ID = "site";
    public static final String FILE_EXTENSION = ".json";
    public static final String SQL_EXTENSION = ".sql";
    public static final String SCRIPT_EXTENSION = ".js";
    // {ID}/ 폴더를 두는 메타데이터의 파일명
    public static final String METADATA_FILE_NAME = "metadata" + FILE_EXTENSION;
    public static final String SQL_FILE_NAME = "query" + SQL_EXTENSION;
    public static final String SCRIPT_FILE_NAME = "script" + SCRIPT_EXTENSION;
    public static final String ACTIONS_DIRECTORY = "actions";
    // 화면 액션 metadata.json 의 kind
    public static final String ACTION_KIND = "ViewAction";

    /**
     * {ID}/ 폴더 안에 놓이는 파일의 종류. METADATA 는 모든 폴더에 있는 메타데이터 본문이다.
     */
    public enum Part {
        METADATA,
        SQL,
        // {ID}/script.js (메시지 프로세서 스크립트). 화면 액션의 script.js 는 ACTION_SCRIPT
        SCRIPT,
        ACTION,
        ACTION_SCRIPT
    }

    private final String path;
    private final String kind;
    private final String idField;
    private final List<String> requiredFields;
    // 비어있지 않으면 메타데이터마다 {ID}/ 폴더를 두고 그 안에 metadata.json 과 이 파일들을 둔다
    private final Set<Part> parts;

    DslFolder(String path, String kind, String idField, List<String> requiredFields) {
        this(path, kind, idField, requiredFields, Set.of());
    }

    DslFolder(String path, String kind, String idField, List<String> requiredFields, Set<Part> parts) {
        this.path = path;
        this.kind = kind;
        this.idField = idField;
        this.requiredFields = requiredFields;
        this.parts = parts;
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

    // 메타데이터마다 {ID}/ 폴더를 두는지
    public boolean hasDirectory() {
        return !parts.isEmpty();
    }

    public boolean has(Part part) {
        return part == Part.METADATA || parts.contains(part);
    }

    public String filePath(String applicationId, String id) {
        return hasDirectory()
                ? directoryOf(applicationId, id) + METADATA_FILE_NAME
                : applicationId + "/" + path + "/" + id + FILE_EXTENSION;
    }

    public String sqlFilePath(String applicationId, String id) {
        return directoryOf(applicationId, id) + SQL_FILE_NAME;
    }

    public String scriptFilePath(String applicationId, String id) {
        return directoryOf(applicationId, id) + SCRIPT_FILE_NAME;
    }

    public String actionFilePath(String applicationId, String id, String actionName) {
        return actionDirectoryOf(applicationId, id, actionName) + METADATA_FILE_NAME;
    }

    public String actionScriptFilePath(String applicationId, String id, String actionName) {
        return actionDirectoryOf(applicationId, id, actionName) + SCRIPT_FILE_NAME;
    }

    private String directoryOf(String applicationId, String id) {
        return applicationId + "/" + path + "/" + id + "/";
    }

    private String actionDirectoryOf(String applicationId, String id, String actionName) {
        return directoryOf(applicationId, id) + ACTIONS_DIRECTORY + "/" + actionName + "/";
    }

    /**
     * DSL 이 관리하는 파일(.json, .sql, .js)인지. 저장소에서 이 확장자가 아닌 파일은 건드리지 않는다.
     */
    public static boolean isDslFile(String path) {
        return path.endsWith(FILE_EXTENSION) || path.endsWith(SQL_EXTENSION) || path.endsWith(SCRIPT_EXTENSION);
    }

    /**
     * 저장소 경로를 폴더/ID/파일 종류로 나눈다. DSL 규칙에 맞는 위치가 아니면 empty.
     * {ID}/ 폴더를 두지 않는 폴더는 {path}/{ID}.json 만, 두는 폴더는 {path}/{ID}/metadata.json 과 그 폴더의 parts 위치만 맞다.
     */
    public static Optional<Location> locate(String applicationId, String filePath) {
        String prefix = applicationId + "/";
        if(!filePath.startsWith(prefix)) {
            return Optional.empty();
        }

        String relative = filePath.substring(prefix.length());
        Optional<DslFolder> found = Arrays.stream(values())
                .filter(folder -> relative.startsWith(folder.path + "/"))
                .findFirst();
        if(found.isEmpty()) {
            return Optional.empty();
        }

        DslFolder folder = found.get();
        String[] names = relative.substring(folder.path.length() + 1).split("/", -1);

        if(!folder.hasDirectory()) {
            if(names.length != 1 || !names[0].endsWith(FILE_EXTENSION)) {
                return Optional.empty();
            }
            return Optional.of(new Location(folder, names[0].substring(0, names[0].length() - FILE_EXTENSION.length()), Part.METADATA, null));
        }

        String id = names[0];
        Location location = null;
        if(names.length == 2 && METADATA_FILE_NAME.equals(names[1])) {
            location = new Location(folder, id, Part.METADATA, null);
        }
        else if(names.length == 2 && SQL_FILE_NAME.equals(names[1])) {
            location = new Location(folder, id, Part.SQL, null);
        }
        else if(names.length == 2 && SCRIPT_FILE_NAME.equals(names[1])) {
            location = new Location(folder, id, Part.SCRIPT, null);
        }
        else if(names.length == 4 && ACTIONS_DIRECTORY.equals(names[1]) && METADATA_FILE_NAME.equals(names[3])) {
            location = new Location(folder, id, Part.ACTION, names[2]);
        }
        else if(names.length == 4 && ACTIONS_DIRECTORY.equals(names[1]) && SCRIPT_FILE_NAME.equals(names[3])) {
            location = new Location(folder, id, Part.ACTION_SCRIPT, names[2]);
        }

        if(location == null || id.isEmpty() || !folder.has(location.part())
                || (location.partName() != null && location.partName().isEmpty())) {
            return Optional.empty();
        }
        return Optional.of(location);
    }

    public static Optional<DslFolder> ofPath(String path) {
        return Arrays.stream(values())
                .filter(folder -> folder.path.equals(path))
                .findFirst();
    }

    /**
     * @param partName 화면 액션(ACTION, ACTION_SCRIPT)이면 actionName, 아니면 null
     */
    public record Location(DslFolder folder, String id, Part part, String partName) {
    }
}
