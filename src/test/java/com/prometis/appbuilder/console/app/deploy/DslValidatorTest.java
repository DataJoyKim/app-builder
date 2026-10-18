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

    // ---- fixture ----

    private static void put(Map<String, String> files, DslFolder folder, String id, Map<String, Object> fields) {
        Map<String, Object> document = DslJson.document(folder);
        document.put(folder.idField(), id);
        document.putAll(fields);
        files.put(folder.filePath(APP, id), DslJson.write(document));
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
