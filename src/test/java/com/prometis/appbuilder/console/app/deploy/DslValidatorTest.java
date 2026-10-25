package com.prometis.appbuilder.console.app.deploy;

import com.prometis.appbuilder.console.app.deploy.dsl.DslFile;
import com.prometis.appbuilder.console.app.deploy.dsl.DslFolder;
import com.prometis.appbuilder.console.app.deploy.dsl.DslIssue;
import com.prometis.appbuilder.console.app.deploy.dsl.DslJson;
import com.prometis.appbuilder.console.app.deploy.dsl.DslValidator;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DSL 검증이 파일명/ID, 참조, 워크플로우 연결을 잡아내는지 확인한다.
 */
class DslValidatorTest {
    private static final String APP = "ehr";

    private final DslValidator validator = new DslValidator();

    @Test
    void 참조가_모두_맞으면_문제가_없다() {
        Map<String, String> files = new LinkedHashMap<>();
        put(files, DslFolder.DATABASE, "MAIN_DB", Map.of("displayName", "메인", "databaseKind", "MYSQL", "url", "jdbc:mysql://x", "username", "u"));
        put(files, DslFolder.QUERY, "Q001", Map.of("displayName", "조회", "dataSourceName", "MAIN_DB"));
        put(files, DslFolder.WORKFLOW, "W001", workflow(
                List.of(node("n1", "SQL", "Q001"), node("n2", "CONDITION", "분기"), node("n3", "ERROR_MESSAGE", "에러")),
                List.of(edge("n1", "n2", "DEFAULT", null), edge("n2", "n3", "CASE", "b1")),
                List.of(Map.of("nodeId", "n2", "branchId", "b1", "conditionExpression", "true")),
                List.of(Map.of("nodeId", "n3", "status", 400))));
        put(files, DslFolder.REST_API, "API001", Map.of("displayName", "API", "httpMethod", "GET", "path", "/a", "workflowCode", "W001", "enabled", true));
        put(files, DslFolder.SCHEDULER, "JOB1", Map.of("jobName", "잡", "cronExpression", "0 0 * * * ?", "enabled", true,
                "workflows", List.of(Map.of("workflowCode", "W001"))));

        List<DslIssue> issues = validator.validate(APP, files);

        assertEquals(List.of(), issues);
    }

    @Test
    void 없는_대상을_가리키면_오류다() {
        Map<String, String> files = new LinkedHashMap<>();
        put(files, DslFolder.QUERY, "Q001", Map.of("displayName", "조회", "dataSourceName", "NO_DB"));
        put(files, DslFolder.WORKFLOW, "W001", workflow(List.of(node("n1", "ENTITY", "NO_ENTITY")), List.of(), List.of(), List.of()));
        put(files, DslFolder.REST_API, "API001", Map.of("displayName", "API", "httpMethod", "GET", "path", "/a", "workflowCode", "NO_WF", "enabled", true));
        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("menuNm", "메뉴");
        menu.put("objectCode", "NO_VIEW");
        put(files, DslFolder.MENU, "M1", menu);

        List<String> messages = errors(validator.validate(APP, files));

        assertTrue(messages.stream().anyMatch(m -> m.contains("Q001") && m.contains("[NO_DB]")), messages.toString());
        assertTrue(messages.stream().anyMatch(m -> m.contains("W001") && m.contains("[NO_ENTITY]")), messages.toString());
        assertTrue(messages.stream().anyMatch(m -> m.contains("API001") && m.contains("[NO_WF]")), messages.toString());
        assertTrue(messages.stream().anyMatch(m -> m.contains("M1") && m.contains("[NO_VIEW]")), messages.toString());
    }

    @Test
    void 파일명과_ID가_다르거나_kind가_틀리면_오류다() {
        Map<String, String> files = new LinkedHashMap<>();
        Map<String, Object> company = DslJson.document(DslFolder.COMPANY);
        company.put("companyCode", "OTHER");
        company.put("companyName", "회사");
        files.put(DslFolder.COMPANY.filePath(APP, "C001"), DslJson.write(company));

        Map<String, Object> wrongKind = DslJson.document(DslFolder.MENU);
        wrongKind.put("companyCode", "C002");
        files.put(DslFolder.COMPANY.filePath(APP, "C002"), DslJson.write(wrongKind));

        files.put(APP + "/unknown/X.json", "{}");
        files.put(DslFolder.COMPANY.filePath(APP, "C003"), "{ broken");

        List<DslIssue> issues = validator.validate(APP, files);

        assertEquals(4, issues.size(), issues.toString());
        assertTrue(issues.stream().allMatch(DslIssue::isError));
    }

    @Test
    void 대소문자만_다른_ID는_오류다() {
        Map<String, String> files = new LinkedHashMap<>();
        put(files, DslFolder.COMPANY, "abc", Map.of("companyName", "a"));
        put(files, DslFolder.COMPANY, "ABC", Map.of("companyName", "b"));

        assertEquals(1, errors(validator.validate(APP, files)).size());
    }

    @Test
    void 끊어진_연결선과_조건없는_분기는_오류다() {
        Map<String, String> files = new LinkedHashMap<>();
        put(files, DslFolder.WORKFLOW, "W001", workflow(
                List.of(node("n1", "CONDITION", "분기"), node("n2", "ERROR_MESSAGE", "에러")),
                List.of(edge("n1", "n2", "CASE", "missing"), edge("n1", "ghost", "DEFAULT", null)),
                List.of(),
                List.of()));

        List<DslIssue> issues = validator.validate(APP, files);

        List<String> errors = errors(issues);
        assertTrue(errors.stream().anyMatch(m -> m.contains("missing")), errors.toString());
        assertTrue(errors.stream().anyMatch(m -> m.contains("ghost")), errors.toString());
        // 에러응답 설정이 없는 에러메시지 노드는 경고
        assertTrue(issues.stream().anyMatch(i -> !i.isError() && i.message().contains("n2")), issues.toString());
    }

    @Test
    void 잘못된_cron과_상위메뉴_순환은_오류다() {
        Map<String, String> files = new LinkedHashMap<>();
        put(files, DslFolder.SCHEDULER, "JOB1", Map.of("jobName", "잡", "cronExpression", "every minute", "enabled", true));
        put(files, DslFolder.MENU, "A", Map.of("menuNm", "a", "parentMenuCd", "B"));
        put(files, DslFolder.MENU, "B", Map.of("menuNm", "b", "parentMenuCd", "A"));

        List<String> errors = errors(validator.validate(APP, files));

        assertTrue(errors.stream().anyMatch(m -> m.contains("cronExpression")), errors.toString());
        assertTrue(errors.stream().anyMatch(m -> m.contains("상위메뉴")), errors.toString());
    }

    @Test
    void 사이트_파일명은_site_로_고정이다() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(DslFolder.SITE.filePath(APP, "layout"), DslJson.write(DslJson.document(DslFolder.SITE)));
        files.put(DslFolder.SITE.filePath(APP, DslFolder.FIXED_SITE_ID), DslJson.write(DslJson.document(DslFolder.SITE)));

        List<DslIssue> issues = validator.validate(APP, files);

        assertEquals(1, issues.size(), issues.toString());
        assertTrue(issues.get(0).path().endsWith("layout.json"));
    }

    @Test
    void DSL_JSON은_줄바꿈이_LF로_고정되고_git_blob_SHA를_계산한다() {
        Map<String, Object> document = DslJson.document(DslFolder.COMPANY);
        document.put("companyCode", "C001");
        document.put("items", List.of());

        String json = DslJson.write(document);

        assertFalse(json.contains("\r"));
        assertTrue(json.startsWith("{\n  \"kind\": \"Company\",\n  \"version\": 1,\n"), json);
        assertTrue(json.endsWith("}\n"));
        // git hash-object 로 확인한 값: printf 'hello\n' | git hash-object --stdin
        assertEquals("ce013625030ba8dba906f756967f9e9ca394464a", DslFile.gitBlobSha("hello\n"));
    }

    @Test
    void 문자열에_담긴_JSON은_풀어서_담고_아니면_그대로_둔다() {
        assertTrue(DslJson.jsonOrText("{\"a\":1}") instanceof com.fasterxml.jackson.databind.JsonNode);
        assertEquals("<div>{</div>", DslJson.jsonOrText("<div>{</div>"));
        assertEquals("{ broken", DslJson.jsonOrText("{ broken"));
        assertNull(DslJson.jsonOrText(null));
    }

    @Test
    void 쿼리는_쿼리명_폴더에_json과_sql이_짝으로_있어야_한다() {
        Map<String, String> files = new LinkedHashMap<>();
        put(files, DslFolder.DATABASE, "MAIN_DB", Map.of("displayName", "DB", "databaseKind", "MYSQL", "url", "jdbc:x", "username", "u"));

        // json 만 있고 sql 이 없다
        Map<String, Object> query = DslJson.document(DslFolder.QUERY);
        query.put("queryName", "Q001");
        query.put("displayName", "조회");
        query.put("dataSourceName", "MAIN_DB");
        files.put(DslFolder.QUERY.filePath(APP, "Q001"), DslJson.write(query));
        // sql 만 있고 json 이 없다
        files.put(DslFolder.QUERY.sqlFilePath(APP, "Q002"), "select 2");
        // 예전 위치(폴더 없이 바로 {queryName}.json)
        files.put(APP + "/workflow/query/Q003.json", "{}");
        // query.sql 이 아닌 이름
        files.put(APP + "/workflow/query/Q004/Q004.sql", "select 4");
        // sql 은 쿼리 폴더에만 둘 수 있다
        files.put(APP + "/security/company/C001.sql", "select 5");

        List<DslIssue> issues = validator.validate(APP, files);
        List<String> paths = issues.stream().map(DslIssue::path).toList();

        assertEquals(5, issues.size(), issues.toString());
        assertTrue(issues.stream().allMatch(DslIssue::isError));
        assertTrue(paths.containsAll(List.of(
                DslFolder.QUERY.filePath(APP, "Q001"),
                DslFolder.QUERY.sqlFilePath(APP, "Q002"),
                APP + "/workflow/query/Q003.json",
                APP + "/workflow/query/Q004/Q004.sql",
                APP + "/security/company/C001.sql")), issues.toString());
    }

    @Test
    void 쿼리_경로는_쿼리명_폴더_아래에_만들어진다() {
        assertEquals(APP + "/workflow/query/Q001/metadata.json", DslFolder.QUERY.filePath(APP, "Q001"));
        assertEquals(APP + "/workflow/query/Q001/query.sql", DslFolder.QUERY.sqlFilePath(APP, "Q001"));
        assertEquals(APP + "/workflow/workflow/W001.json", DslFolder.WORKFLOW.filePath(APP, "W001"));

        DslFolder.Location sql = DslFolder.locate(APP, APP + "/workflow/query/Q001/query.sql").orElseThrow();
        assertEquals(DslFolder.QUERY, sql.folder());
        assertEquals("Q001", sql.id());
        assertEquals(DslFolder.Part.SQL, sql.part());
        assertTrue(DslFolder.locate(APP, APP + "/workflow/query/Q001.json").isEmpty());
        assertTrue(DslFolder.locate(APP, APP + "/workflow/workflow/W001/W001.json").isEmpty());
        assertTrue(DslFolder.locate(APP, APP + "/workflow/query/Q001/Q001.json").isEmpty());
        assertEquals("Q001", DslFolder.locate(APP, APP + "/workflow/query/Q001/metadata.json").orElseThrow().id());
    }

    @Test
    void 화면_액션은_화면폴더의_actions_아래에_있어야_한다() {
        Map<String, String> files = new LinkedHashMap<>();
        put(files, DslFolder.VIEW_OBJECT, "V001", Map.of("objectName", "화면"));

        // 맞는 액션 + 스크립트
        files.put(DslFolder.VIEW_OBJECT.actionFilePath(APP, "V001", "save"), DslJson.write(action("save")));
        files.put(DslFolder.VIEW_OBJECT.actionScriptFilePath(APP, "V001", "save"), "alert(1);");
        assertEquals(List.of(), validator.validate(APP, files));

        // 폴더명과 actionName 이 다르다
        files.put(DslFolder.VIEW_OBJECT.actionFilePath(APP, "V001", "edit"), DslJson.write(action("other")));
        // 액션 metadata.json 없이 스크립트만 있다
        files.put(DslFolder.VIEW_OBJECT.actionScriptFilePath(APP, "V001", "delete"), "alert(2);");
        // 화면 metadata.json 이 없다
        files.put(DslFolder.VIEW_OBJECT.actionFilePath(APP, "V002", "save"), DslJson.write(action("save")));
        // kind 가 틀리다
        files.put(DslFolder.VIEW_OBJECT.actionFilePath(APP, "V001", "wrongKind"), DslJson.write(DslJson.document(DslFolder.VIEW_OBJECT)));
        // 대소문자만 다르다
        files.put(DslFolder.VIEW_OBJECT.actionFilePath(APP, "V001", "SAVE"), DslJson.write(action("SAVE")));
        // 예전 위치
        files.put(APP + "/view/view-object/V003.json", "{}");

        List<DslIssue> issues = validator.validate(APP, files);
        List<String> paths = issues.stream().map(DslIssue::path).toList();

        assertEquals(6, issues.size(), issues.toString());
        assertTrue(issues.stream().allMatch(DslIssue::isError));
        assertTrue(paths.containsAll(List.of(
                DslFolder.VIEW_OBJECT.actionFilePath(APP, "V001", "edit"),
                DslFolder.VIEW_OBJECT.actionScriptFilePath(APP, "V001", "delete"),
                DslFolder.VIEW_OBJECT.actionFilePath(APP, "V002", "save"),
                DslFolder.VIEW_OBJECT.actionFilePath(APP, "V001", "wrongKind"),
                APP + "/view/view-object/V003.json")), issues.toString());
    }

    @Test
    void 화면_경로는_화면코드_폴더_아래에_만들어진다() {
        assertEquals(APP + "/view/view-object/V001/metadata.json", DslFolder.VIEW_OBJECT.filePath(APP, "V001"));
        assertEquals(APP + "/view/view-object/V001/actions/save/metadata.json", DslFolder.VIEW_OBJECT.actionFilePath(APP, "V001", "save"));
        assertEquals(APP + "/view/view-object/V001/actions/save/script.js", DslFolder.VIEW_OBJECT.actionScriptFilePath(APP, "V001", "save"));

        DslFolder.Location script = DslFolder.locate(APP, APP + "/view/view-object/V001/actions/save/script.js").orElseThrow();
        assertEquals(DslFolder.VIEW_OBJECT, script.folder());
        assertEquals("V001", script.id());
        assertEquals(DslFolder.Part.ACTION_SCRIPT, script.part());
        assertEquals("save", script.partName());

        // 화면에는 query.sql 이, 쿼리에는 actions 가 없다
        assertTrue(DslFolder.locate(APP, APP + "/view/view-object/V001/query.sql").isEmpty());
        assertTrue(DslFolder.locate(APP, APP + "/workflow/query/Q001/actions/save/metadata.json").isEmpty());
        assertTrue(DslFolder.locate(APP, APP + "/view/view-object/V001/actions/save/other.js").isEmpty());
    }

    private static Map<String, Object> action(String actionName) {
        Map<String, Object> action = DslJson.document(DslFolder.ACTION_KIND);
        action.put("actionName", actionName);
        action.put("displayName", "액션");
        return action;
    }

    // ---- fixture ----

    private static void put(Map<String, String> files, DslFolder folder, String id, Map<String, Object> fields) {
        Map<String, Object> document = DslJson.document(folder);
        document.put(folder.idField(), id);
        document.putAll(fields);
        files.put(folder.filePath(APP, id), DslJson.write(document));
        if(folder.has(DslFolder.Part.SQL)) {
            files.put(folder.sqlFilePath(APP, id), "select 1");
        }
    }

    private static Map<String, Object> workflow(List<Map<String, Object>> nodes, List<Map<String, Object>> edges,
                                                List<Map<String, Object>> conditions, List<Map<String, Object>> errorResponses) {
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("displayName", "워크플로우");
        workflow.put("useAuthValidation", false);
        workflow.put("nodes", nodes);
        workflow.put("edges", edges);
        workflow.put("conditions", conditions);
        workflow.put("errorResponses", errorResponses);
        return workflow;
    }

    private static Map<String, Object> node(String nodeId, String functionType, String functionName) {
        return Map.of("nodeId", nodeId, "functionType", functionType, "functionName", functionName);
    }

    private static Map<String, Object> edge(String source, String target, String branchType, String branchId) {
        Map<String, Object> edge = new HashMap<>();
        edge.put("sourceNodeId", source);
        edge.put("targetNodeId", target);
        edge.put("branchType", branchType);
        edge.put("branchId", branchId);
        return edge;
    }

    private static List<String> errors(List<DslIssue> issues) {
        return issues.stream().filter(DslIssue::isError).map(issue -> issue.path() + " " + issue.message()).toList();
    }
}
