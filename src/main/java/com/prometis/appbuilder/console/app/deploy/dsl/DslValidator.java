package com.prometis.appbuilder.console.app.deploy.dsl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.quartz.CronExpression;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * DSL 파일 묶음을 검증한다. 메타데이터 DB 가 아니라 DSL 파일 자체만 보고 판단하므로,
 * 커밋 전(DB 에서 만든 DSL)과 배포 전(저장소에서 읽은 DSL) 모두 같은 규칙으로 검사할 수 있다.
 *
 * ERROR : 이대로 올리면 배포 환경에서 동작하지 않는 것 (깨진 JSON, 파일명과 ID 불일치, 없는 대상 참조, 끊어진 워크플로우 연결 등)
 * WARNING : 동작은 하지만 확인이 필요한 것 (DSL 대상이 아닌 파일 핸들러 참조, 시작 노드가 여럿인 워크플로우 등)
 */
@Component
public class DslValidator {
    // 윈도우/맥에서 체크아웃할 수 없거나 경로를 바꿔버리는 글자
    private static final Pattern UNSAFE_ID = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");
    private static final int MAX_ID_LENGTH = 100;

    // 워크플로우 노드 기능유형 -> functionName 이 가리키는 DSL 폴더
    private static final Map<String, DslFolder> FUNCTION_TARGETS = Map.of(
            "ENTITY", DslFolder.ENTITY,
            "SQL", DslFolder.QUERY,
            "REST_CLIENT", DslFolder.REST_CLIENT,
            "MESSAGE_PROCESSOR", DslFolder.MESSAGE_PROCESSOR,
            "NOTIFICATION", DslFolder.NOTIFICATION
    );

    /**
     * @param files 경로 -> JSON 본문. 경로는 저장소 최상위 기준({applicationId}/...)이다.
     */
    public List<DslIssue> validate(String applicationId, Map<String, String> files) {
        Context context = new Context();

        List<String> paths = new ArrayList<>(files.keySet());
        Collections.sort(paths);

        for(String path : paths) {
            readDocument(applicationId, path, files.get(path), context);
        }

        checkDuplicateIds(context);

        for(Document document : context.documents) {
            checkRequiredFields(document, context);
            checkReferences(document, context);
        }

        checkRestApiRoutes(context);
        checkMenuCycles(context);

        return context.issues;
    }

    public static boolean hasError(List<DslIssue> issues) {
        return issues.stream().anyMatch(DslIssue::isError);
    }

    // ---- 파일 단위 ----

    private void readDocument(String applicationId, String path, String content, Context context) {
        String prefix = applicationId + "/";
        if(!path.startsWith(prefix) || !path.endsWith(DslFolder.FILE_EXTENSION)) {
            context.error(path, "애플리케이션 폴더(" + prefix + ") 아래의 .json 파일이 아닙니다.");
            return;
        }

        String relative = path.substring(prefix.length());
        int slash = relative.lastIndexOf('/');
        String folderPath = slash < 0 ? "" : relative.substring(0, slash);
        String id = relative.substring(slash + 1, relative.length() - DslFolder.FILE_EXTENSION.length());

        Optional<DslFolder> folder = DslFolder.ofPath(folderPath);
        if(folder.isEmpty()) {
            context.error(path, "알 수 없는 DSL 폴더입니다. [" + folderPath + "]");
            return;
        }

        JsonNode node;
        try {
            node = DslJson.read(content);
        }
        catch (JsonProcessingException e) {
            context.error(path, "JSON 형식이 올바르지 않습니다. " + e.getOriginalMessage());
            return;
        }

        if(node == null || !node.isObject()) {
            context.error(path, "JSON 객체가 아닙니다.");
            return;
        }

        DslFolder dslFolder = folder.get();
        String kind = text(node, "kind");
        if(!dslFolder.kind().equals(kind)) {
            context.error(path, "kind 가 폴더와 맞지 않습니다. (기대값: " + dslFolder.kind() + ", 실제값: " + kind + ")");
            return;
        }

        if(!checkId(path, dslFolder, id, node, context)) {
            return;
        }

        Document document = new Document(path, dslFolder, id, node);
        context.documents.add(document);
        context.byFolder.computeIfAbsent(dslFolder, f -> new LinkedHashMap<>()).put(id, document);
    }

    private boolean checkId(String path, DslFolder folder, String fileId, JsonNode node, Context context) {
        if(folder.idField() == null) {
            if(!DslFolder.FIXED_SITE_ID.equals(fileId)) {
                context.error(path, "파일명은 " + DslFolder.FIXED_SITE_ID + DslFolder.FILE_EXTENSION + " 이어야 합니다.");
                return false;
            }
            return true;
        }

        String id = text(node, folder.idField());
        if(isBlank(id)) {
            context.error(path, folder.idField() + " 값이 없습니다.");
            return false;
        }

        if(!id.equals(fileId)) {
            context.error(path, "파일명과 " + folder.idField() + " 가 다릅니다. (" + folder.idField() + ": " + id + ")");
            return false;
        }

        if(id.length() > MAX_ID_LENGTH) {
            context.error(path, folder.idField() + " 는 " + MAX_ID_LENGTH + "자를 넘을 수 없습니다.");
            return false;
        }

        if(UNSAFE_ID.matcher(id).find() || id.equals(".") || id.equals("..") || !id.equals(id.trim())) {
            context.error(path, folder.idField() + " 에 파일명으로 쓸 수 없는 문자(\\ / : * ? \" < > | 앞뒤 공백 등)가 있습니다. [" + id + "]");
            return false;
        }

        return true;
    }

    // 대소문자만 다른 파일은 윈도우/맥에서 같은 파일로 겹친다
    private void checkDuplicateIds(Context context) {
        context.byFolder.forEach((folder, documents) -> {
            Map<String, String> seen = new HashMap<>();
            documents.keySet().forEach(id -> {
                String lower = id.toLowerCase(Locale.ROOT);
                String other = seen.putIfAbsent(lower, id);
                if(other != null) {
                    context.error(documents.get(id).path, "대소문자만 다른 ID 가 있습니다. [" + other + ", " + id + "] 운영체제에 따라 같은 파일로 겹칩니다.");
                }
            });
        });
    }

    private void checkRequiredFields(Document document, Context context) {
        for(String field : document.folder.requiredFields()) {
            JsonNode value = document.node.get(field);
            if(value == null || value.isNull() || (value.isTextual() && value.asText().isBlank())) {
                context.error(document.path, field + " 값이 없습니다.");
            }
        }
    }

    // ---- 참조 ----

    private void checkReferences(Document document, Context context) {
        JsonNode node = document.node;

        switch (document.folder) {
            case QUERY, ENTITY -> requireReference(document, "dataSourceName", text(node, "dataSourceName"), DslFolder.DATABASE, context);
            case REST_CLIENT -> requireReference(document, "dataSourceName", text(node, "dataSourceName"), DslFolder.REST_SERVER, context);
            case NOTIFICATION -> requireReference(document, "dataSourceName", text(node, "dataSourceName"), DslFolder.NOTIFICATION_PROVIDER, context);
            case WORKFLOW -> checkWorkflow(document, context);
            case REST_API -> requireReference(document, "workflowCode", text(node, "workflowCode"), DslFolder.WORKFLOW, context);
            case SCHEDULER -> checkScheduler(document, context);
            case MENU -> {
                optionalReference(document, "objectCode", text(node, "objectCode"), DslFolder.VIEW_OBJECT, context);
                optionalReference(document, "parentMenuCd", text(node, "parentMenuCd"), DslFolder.MENU, context);
            }
            case SITE -> {
                String homeObjectCode = text(node, "homeObjectCode");
                if(!isBlank(homeObjectCode) && !context.exists(DslFolder.VIEW_OBJECT, homeObjectCode)) {
                    context.warning(document.path, "homeObjectCode 가 가리키는 화면이 없습니다. [" + homeObjectCode + "]");
                }
            }
            case IMAGE_STORAGE_TYPE -> checkUnique(document, "images", "filename", context);
            case COMMON_CODE -> checkUnique(document, "codes", "code", context);
            default -> {
            }
        }
    }

    private void checkWorkflow(Document document, Context context) {
        JsonNode node = document.node;

        Map<String, String> nodeTypes = new LinkedHashMap<>();
        for(JsonNode workflowNode : array(node, "nodes")) {
            String nodeId = text(workflowNode, "nodeId");
            String functionType = text(workflowNode, "functionType");
            String functionName = text(workflowNode, "functionName");

            if(isBlank(nodeId)) {
                context.error(document.path, "nodeId 가 없는 노드가 있습니다. [" + functionName + "]");
                continue;
            }
            if(nodeTypes.containsKey(nodeId)) {
                context.error(document.path, "nodeId 가 중복됩니다. [" + nodeId + "]");
                continue;
            }
            nodeTypes.put(nodeId, functionType);

            DslFolder target = FUNCTION_TARGETS.get(functionType);
            if(target != null) {
                if(!context.exists(target, functionName)) {
                    context.error(document.path, "노드 [" + nodeId + "] 가 가리키는 " + target.kind() + " 가 없습니다. [" + functionName + "]");
                }
            }
            else if("FILE".equals(functionType)) {
                context.warning(document.path, "노드 [" + nodeId + "] 의 파일 핸들러 [" + functionName + "] 는 DSL 대상이 아닙니다. 배포 환경에 따로 등록되어 있어야 합니다.");
            }
        }

        Set<String> conditionBranches = new HashSet<>();
        for(JsonNode condition : array(node, "conditions")) {
            String nodeId = text(condition, "nodeId");
            if(!"CONDITION".equals(nodeTypes.get(nodeId))) {
                context.error(document.path, "조건이 조건분기 노드가 아닌 곳에 있습니다. [" + nodeId + "]");
            }
            conditionBranches.add(nodeId + "\n" + text(condition, "branchId"));
        }

        Set<String> targets = new HashSet<>();
        for(JsonNode edge : array(node, "edges")) {
            String source = text(edge, "sourceNodeId");
            String target = text(edge, "targetNodeId");

            if(!nodeTypes.containsKey(source) || !nodeTypes.containsKey(target)) {
                context.error(document.path, "연결선이 없는 노드를 가리킵니다. [" + source + " -> " + target + "]");
                continue;
            }
            targets.add(target);

            if("CASE".equals(text(edge, "branchType")) && !conditionBranches.contains(source + "\n" + text(edge, "branchId"))) {
                context.error(document.path, "조건분기 연결선 [" + source + " -> " + target + "] 의 조건(branchId: " + text(edge, "branchId") + ")이 없습니다.");
            }
        }

        Set<String> errorNodes = new HashSet<>();
        for(JsonNode errorResponse : array(node, "errorResponses")) {
            String nodeId = text(errorResponse, "nodeId");
            if(!"ERROR_MESSAGE".equals(nodeTypes.get(nodeId))) {
                context.error(document.path, "에러응답이 에러메시지 노드가 아닌 곳에 있습니다. [" + nodeId + "]");
            }
            errorNodes.add(nodeId);
        }

        nodeTypes.forEach((nodeId, functionType) -> {
            if("ERROR_MESSAGE".equals(functionType) && !errorNodes.contains(nodeId)) {
                context.warning(document.path, "에러메시지 노드 [" + nodeId + "] 에 에러응답 설정이 없습니다.");
            }
        });

        // 연결선이 있으면 들어오는 연결선이 없는 노드에서 시작한다. 여럿이면 어디서 시작할지 모호하다
        if(!array(node, "edges").isEmpty()) {
            List<String> starts = nodeTypes.keySet().stream().filter(nodeId -> !targets.contains(nodeId)).toList();
            if(starts.size() > 1) {
                context.warning(document.path, "시작 노드(들어오는 연결선이 없는 노드)가 여러 개입니다. " + starts);
            }
        }
    }

    private void checkScheduler(Document document, Context context) {
        String cron = text(document.node, "cronExpression");
        if(!isBlank(cron) && !CronExpression.isValidExpression(cron)) {
            context.error(document.path, "cronExpression 이 올바르지 않습니다. [" + cron + "]");
        }

        for(JsonNode workflow : array(document.node, "workflows")) {
            requireReference(document, "workflows.workflowCode", text(workflow, "workflowCode"), DslFolder.WORKFLOW, context);
        }
    }

    private void checkUnique(Document document, String arrayField, String keyField, Context context) {
        Set<String> seen = new HashSet<>();
        for(JsonNode item : array(document.node, arrayField)) {
            String key = text(item, keyField);
            if(isBlank(key)) {
                context.error(document.path, arrayField + " 에 " + keyField + " 가 없는 항목이 있습니다.");
            }
            else if(!seen.add(key)) {
                context.error(document.path, arrayField + " 의 " + keyField + " 가 중복됩니다. [" + key + "]");
            }
        }
    }

    // 같은 메소드 + 경로의 REST API 가 둘이면 어느 쪽이 호출될지 모른다
    private void checkRestApiRoutes(Context context) {
        Map<String, String> routes = new HashMap<>();
        context.byFolder.getOrDefault(DslFolder.REST_API, Map.of()).values().forEach(document -> {
            String route = text(document.node, "httpMethod") + " " + text(document.node, "path");
            String other = routes.putIfAbsent(route, document.id);
            if(other != null) {
                context.error(document.path, "같은 메소드와 경로의 REST API 가 있습니다. [" + route + "] (" + other + ")");
            }
        });
    }

    private void checkMenuCycles(Context context) {
        Map<String, Document> menus = context.byFolder.getOrDefault(DslFolder.MENU, Map.of());
        for(Document menu : menus.values()) {
            Set<String> visited = new HashSet<>();
            String current = menu.id;
            while(current != null && menus.containsKey(current)) {
                if(!visited.add(current)) {
                    context.error(menu.path, "상위메뉴(parentMenuCd)가 돌고 돕니다. " + visited);
                    break;
                }
                current = text(menus.get(current).node, "parentMenuCd");
            }
        }
    }

    private void requireReference(Document document, String field, String id, DslFolder target, Context context) {
        if(isBlank(id)) {
            context.error(document.path, field + " 값이 없습니다.");
        }
        else if(!context.exists(target, id)) {
            context.error(document.path, field + " 가 가리키는 " + target.kind() + " 가 없습니다. [" + id + "]");
        }
    }

    private void optionalReference(Document document, String field, String id, DslFolder target, Context context) {
        if(!isBlank(id) && !context.exists(target, id)) {
            context.error(document.path, field + " 가 가리키는 " + target.kind() + " 가 없습니다. [" + id + "]");
        }
    }

    // ---- util ----

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    private static List<JsonNode> array(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if(value == null || !value.isArray()) {
            return List.of();
        }

        List<JsonNode> items = new ArrayList<>();
        value.forEach(items::add);
        return items;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Document(String path, DslFolder folder, String id, JsonNode node) {
    }

    private static class Context {
        private final List<DslIssue> issues = new ArrayList<>();
        private final List<Document> documents = new ArrayList<>();
        private final Map<DslFolder, Map<String, Document>> byFolder = new EnumMap<>(DslFolder.class);

        boolean exists(DslFolder folder, String id) {
            return id != null && byFolder.getOrDefault(folder, Map.of()).containsKey(id);
        }

        void error(String path, String message) {
            issues.add(DslIssue.error(path, message));
        }

        void warning(String path, String message) {
            issues.add(DslIssue.warning(path, message));
        }
    }
}
