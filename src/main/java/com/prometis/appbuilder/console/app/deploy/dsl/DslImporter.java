package com.prometis.appbuilder.console.app.deploy.dsl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.prometis.appbuilder.app.code.CommonCode;
import com.prometis.appbuilder.app.code.CommonCodeKind;
import com.prometis.appbuilder.app.code.CommonCodeKindRepository;
import com.prometis.appbuilder.app.code.CommonCodeRepository;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMeta;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMetaRepository;
import com.prometis.appbuilder.app.datasource.notification.NotificationProvider;
import com.prometis.appbuilder.app.datasource.notification.NotificationProviderRepository;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServer;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServerRepository;
import com.prometis.appbuilder.app.entity.Entity;
import com.prometis.appbuilder.app.entity.EntityColumn;
import com.prometis.appbuilder.app.entity.EntityRepository;
import com.prometis.appbuilder.app.message.MessageProcessor;
import com.prometis.appbuilder.app.message.MessageProcessorRepository;
import com.prometis.appbuilder.app.node.WorkflowNode;
import com.prometis.appbuilder.app.node.WorkflowNodeRepository;
import com.prometis.appbuilder.app.notification.Notification;
import com.prometis.appbuilder.app.notification.NotificationRepository;
import com.prometis.appbuilder.app.query.Query;
import com.prometis.appbuilder.app.query.QueryParam;
import com.prometis.appbuilder.app.query.QueryRepository;
import com.prometis.appbuilder.app.restapi.RestApi;
import com.prometis.appbuilder.app.restapi.RestApiRepository;
import com.prometis.appbuilder.app.restapi.RestApiService;
import com.prometis.appbuilder.app.restapi.RestApiValidationException;
import com.prometis.appbuilder.app.restclient.RestClient;
import com.prometis.appbuilder.app.restclient.RestClientBody;
import com.prometis.appbuilder.app.restclient.RestClientHeader;
import com.prometis.appbuilder.app.restclient.RestClientQueryParam;
import com.prometis.appbuilder.app.restclient.RestClientRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerJobHistoryRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerJobRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerJobWorkflowRepository;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJob;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJobWorkflow;
import com.prometis.appbuilder.app.view.LayoutRepository;
import com.prometis.appbuilder.app.view.MenuPermission;
import com.prometis.appbuilder.app.view.MenuPermissionRepository;
import com.prometis.appbuilder.app.view.MenuRepository;
import com.prometis.appbuilder.app.view.ViewActionRepository;
import com.prometis.appbuilder.app.view.ViewCodeRepository;
import com.prometis.appbuilder.app.view.ViewObjectContentRepository;
import com.prometis.appbuilder.app.view.ViewObjectRepository;
import com.prometis.appbuilder.app.view.code.ObjectType;
import com.prometis.appbuilder.app.view.domain.Layout;
import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.domain.ViewAction;
import com.prometis.appbuilder.app.view.domain.ViewCode;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.appbuilder.app.view.domain.ViewObjectContent;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowCondition;
import com.prometis.appbuilder.app.workflow.WorkflowConditionRepository;
import com.prometis.appbuilder.app.workflow.WorkflowDomain;
import com.prometis.appbuilder.app.workflow.WorkflowDomainRepository;
import com.prometis.appbuilder.app.workflow.WorkflowEdge;
import com.prometis.appbuilder.app.workflow.WorkflowEdgeRepository;
import com.prometis.appbuilder.app.workflow.WorkflowErrorResponse;
import com.prometis.appbuilder.app.workflow.WorkflowErrorResponseRepository;
import com.prometis.appbuilder.app.workflow.WorkflowIpGroup;
import com.prometis.appbuilder.app.workflow.WorkflowIpGroupRepository;
import com.prometis.appbuilder.app.workflow.WorkflowPermission;
import com.prometis.appbuilder.app.workflow.WorkflowPermissionRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import com.prometis.appbuilder.console.app.deploy.DeployException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

/**
 * DSL 파일(저장소의 {applicationId}/...)을 애플리케이션 메타데이터 DB 에 반영한다. DslExporter 의 반대 방향이다.
 *
 * - 메타데이터 하나(종류 + ID) 단위로 만들기/고치기/지우기를 한다. 메타데이터는 ID(코드, 이름)로 찾고, DB id 는 이 환경의 것을 그대로 쓴다.
 * - 한 트랜잭션에서 처리한다. 중간에 하나라도 실패하면 아무것도 바뀌지 않는다.
 * - 순서: (REST API 삭제) -> 다른 것이 가리키는 것부터 저장(데이터소스 -> ... -> 워크플로우 -> REST API/스케줄러 -> 화면 -> 메뉴 -> 사이트)
 *         -> 나머지 삭제는 반대 순서. REST API 는 메소드+경로가 겹치는 새 API 를 만들 수 있게 삭제를 먼저 한다.
 * - DSL 에 없는 민감정보는 이 환경의 값을 그대로 둔다. (데이터베이스 password, 알림 제공자 options 의 password/secret/token/credential 키)
 * - 디스크(FILE 화면 소스)와 스케줄러(Quartz)는 트랜잭션에 묶이지 않으므로 할 일(PostActions)만 돌려주고, 호출하는 쪽이 커밋 뒤에 실행한다.
 */
@Component
@RequiredArgsConstructor
public class DslImporter {
    private static final Pattern SECRET_OPTION_KEY = Pattern.compile("(?i).*(password|secret|token|credential).*");

    // 엔티티에 setter 가 없어서 필드에 바로 넣는다. DSL 의 필드명은 엔티티 필드명과 같다(DslExporter)
    private static final ObjectMapper ENTITY_MAPPER = JsonMapper.builder()
            .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    // 저장 순서. 삭제는 이 반대 순서 (REST_API 만 맨 앞에서 삭제)
    private static final List<DslFolder> SAVE_ORDER = List.of(
            DslFolder.DATABASE, DslFolder.REST_SERVER, DslFolder.NOTIFICATION_PROVIDER,
            DslFolder.COMMON_CODE,
            DslFolder.QUERY, DslFolder.ENTITY, DslFolder.REST_CLIENT, DslFolder.NOTIFICATION, DslFolder.MESSAGE_PROCESSOR,
            DslFolder.WORKFLOW,
            DslFolder.REST_API, DslFolder.SCHEDULER,
            DslFolder.VIEW_OBJECT, DslFolder.MENU, DslFolder.SITE
    );

    private final EntityManager entityManager;

    private final DataSourceDatabaseMetaRepository databaseRepository;
    private final DataSourceRestServerRepository restServerRepository;
    private final NotificationProviderRepository notificationProviderRepository;

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final WorkflowEdgeRepository workflowEdgeRepository;
    private final WorkflowConditionRepository workflowConditionRepository;
    private final WorkflowErrorResponseRepository workflowErrorResponseRepository;
    private final WorkflowPermissionRepository workflowPermissionRepository;
    private final WorkflowIpGroupRepository workflowIpGroupRepository;
    private final WorkflowDomainRepository workflowDomainRepository;
    private final QueryRepository queryRepository;
    private final EntityRepository entityRepository;
    private final RestClientRepository restClientRepository;
    private final NotificationRepository notificationRepository;
    private final MessageProcessorRepository messageProcessorRepository;

    private final RestApiRepository restApiRepository;
    private final RestApiService restApiService;

    private final SchedulerJobRepository schedulerJobRepository;
    private final SchedulerJobWorkflowRepository schedulerJobWorkflowRepository;
    private final SchedulerJobHistoryRepository schedulerJobHistoryRepository;

    private final ViewObjectRepository viewObjectRepository;
    private final ViewObjectContentRepository viewObjectContentRepository;
    private final ViewActionRepository viewActionRepository;
    private final ViewCodeRepository viewCodeRepository;
    private final MenuRepository menuRepository;
    private final MenuPermissionRepository menuPermissionRepository;
    private final LayoutRepository layoutRepository;

    private final CommonCodeKindRepository commonCodeKindRepository;
    private final CommonCodeRepository commonCodeRepository;

    public enum Action {
        CREATE, UPDATE, DELETE
    }

    /**
     * 메타데이터 하나의 DSL. metadata 는 metadata.json(또는 {ID}.json), body 는 query.sql / script.js 본문, page 는 page.html.
     */
    public record DslDocument(DslFolder folder, String id, JsonNode metadata, String body, String page, List<ActionDocument> actions) {
    }

    public record ActionDocument(String actionName, JsonNode metadata, String script) {
    }

    /**
     * 반영할 변경 하나. DELETE 면 document 는 null.
     */
    public record Change(DslFolder folder, String id, Action action, DslDocument document) {
    }

    /**
     * 트랜잭션 밖(커밋 뒤)에서 할 일.
     */
    public record PostActions(
            Map<String, String> viewSourceWrites,
            List<String> viewSourceDeletes,
            List<SchedulerJob> schedulerJobs,
            List<String> unscheduleJobCodes,
            List<String> warnings
    ) {
    }

    public record Outcome(int created, int updated, int deleted, PostActions postActions) {
    }

    /**
     * DB 에 반영하기 전에 미리 알 수 있는 문제. 미리보기에서 오류로 보여준다.
     */
    @Transactional(readOnly = true)
    public List<DslIssue> check(String applicationId, List<Change> changes) {
        List<DslIssue> issues = new ArrayList<>();

        for(Change change : changes) {
            if(change.action() == Action.DELETE) {
                continue;
            }

            if(change.folder() == DslFolder.VIEW_OBJECT && change.action() == Action.UPDATE) {
                ObjectType type = enumOf(ObjectType.class, text(change.document().metadata(), "type"));
                viewObjectRepository.findByApplicationIdAndObjectCode(applicationId, change.id())
                        .filter(saved -> type != null && saved.getType() != null && saved.getType() != type)
                        .ifPresent(saved -> issues.add(DslIssue.error(change.id(),
                                "화면 '" + change.id() + "' 의 유형이 다릅니다 (이 환경: " + saved.getType() + ", 저장소: " + type + "). 유형은 만든 뒤 바꿀 수 없으니 이 환경에서 화면을 지운 뒤 다시 배포하세요.")));
            }

            if(change.folder() == DslFolder.SCHEDULER) {
                schedulerJobRepository.findByJobCode(change.id())
                        .filter(job -> !applicationId.equals(job.getApplicationId()))
                        .ifPresent(job -> issues.add(DslIssue.error(change.id(),
                                "스케줄러 작업코드 '" + change.id() + "' 를 다른 애플리케이션(" + job.getApplicationId() + ")이 쓰고 있습니다. 작업코드는 서버 전체에서 하나만 쓸 수 있습니다.")));
            }
        }

        return issues;
    }

    @Transactional(rollbackFor = Exception.class)
    public Outcome apply(String applicationId, List<Change> changes) {
        Context context = new Context(applicationId);

        Map<DslFolder, List<Change>> byFolder = new EnumMap<>(DslFolder.class);
        changes.forEach(change -> byFolder.computeIfAbsent(change.folder(), f -> new ArrayList<>()).add(change));

        // 1) REST API 삭제 (메소드+경로가 같은 새 API 를 만들 수 있도록 먼저)
        for(Change change : byFolder.getOrDefault(DslFolder.REST_API, List.of())) {
            if(change.action() == Action.DELETE) {
                run(change, () -> deleteRestApi(context, change.id()));
                context.deleted++;
            }
        }

        // 2) 만들기/고치기
        for(DslFolder folder : SAVE_ORDER) {
            List<Change> saves = byFolder.getOrDefault(folder, List.of()).stream()
                    .filter(change -> change.action() != Action.DELETE)
                    .toList();
            if(saves.isEmpty()) {
                continue;
            }

            if(folder == DslFolder.MENU) {
                run(saves.get(0), () -> saveMenus(context, saves));
            }
            else {
                for(Change change : saves) {
                    run(change, () -> save(context, change));
                }
            }
            saves.forEach(change -> {
                if(change.action() == Action.CREATE) context.created++;
                else context.updated++;
            });
        }

        // 3) 나머지 삭제 (반대 순서)
        entityManager.flush();
        // 메뉴 하위목록(children)이 위에서 바꾼 상위 메뉴를 반영하도록 영속성 컨텍스트를 비우고 다시 읽는다.
        // 그대로 두면 지우는 메뉴의 옛 children 에 남은(다른 곳으로 옮긴) 메뉴까지 cascade 로 지워진다
        entityManager.clear();

        List<DslFolder> deleteOrder = new ArrayList<>(SAVE_ORDER);
        Collections.reverse(deleteOrder);
        for(DslFolder folder : deleteOrder) {
            if(folder == DslFolder.REST_API) {
                continue;
            }
            List<Change> deletes = byFolder.getOrDefault(folder, List.of()).stream()
                    .filter(change -> change.action() == Action.DELETE)
                    .toList();
            if(deletes.isEmpty()) {
                continue;
            }

            if(folder == DslFolder.MENU) {
                run(deletes.get(0), () -> deleteMenus(context, deletes.stream().map(Change::id).toList()));
            }
            else {
                for(Change change : deletes) {
                    run(change, () -> delete(context, change));
                }
            }
            context.deleted += deletes.size();
        }

        entityManager.flush();
        return new Outcome(context.created, context.updated, context.deleted, context.postActions());
    }

    // 어떤 메타데이터에서 실패했는지 알 수 있게 메시지를 붙여서 다시 던진다 (런타임 예외라 트랜잭션은 롤백된다)
    private static void run(Change change, Runnable task) {
        try {
            task.run();
        }
        catch (DeployException e) {
            throw e;
        }
        catch (RuntimeException e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw DeployException.badRequest("[" + change.folder().kind() + " " + change.id() + "] 반영하지 못했습니다. " + message);
        }
    }

    // ================= 저장 =================

    private void save(Context context, Change change) {
        DslDocument document = change.document();
        String applicationId = context.applicationId;
        String id = change.id();
        JsonNode metadata = document.metadata();

        switch (change.folder()) {
            case DATABASE -> {
                Optional<DataSourceDatabaseMeta> saved = databaseRepository.findByApplicationId(applicationId).stream()
                        .filter(meta -> id.equals(meta.getDataSourceName())).findFirst();
                ObjectNode node = root(metadata, applicationId, saved.map(DataSourceDatabaseMeta::getId).orElse(null));
                // 비밀번호는 DSL 에 없다. 이 환경의 값을 그대로 두고, 새로 만드는 데이터베이스는 빈 값으로 둔다 (컬럼이 NOT NULL)
                String password = saved.map(DataSourceDatabaseMeta::getPassword).orElse("");
                node.put("password", password);
                databaseRepository.save(convert(node, DataSourceDatabaseMeta.class));
                if(password.isEmpty()) {
                    context.warnings.add("데이터베이스 '" + id + "' 의 비밀번호가 없습니다. [데이터베이스] 화면에서 비밀번호를 입력한 뒤 새로고침하세요.");
                }
                else {
                    context.warnings.add("데이터베이스 '" + id + "' 가 바뀌었습니다. [데이터베이스] 화면에서 새로고침해야 연결에 반영됩니다.");
                }
            }
            case REST_SERVER -> {
                Long savedId = restServerRepository.findByApplicationId(applicationId).stream()
                        .filter(server -> id.equals(server.getDataSourceName())).findFirst()
                        .map(DataSourceRestServer::getId).orElse(null);
                restServerRepository.save(convert(root(metadata, applicationId, savedId), DataSourceRestServer.class));
                context.warnings.add("REST 서버 '" + id + "' 가 바뀌었습니다. [REST 서버] 화면에서 새로고침해야 연결에 반영됩니다.");
            }
            case NOTIFICATION_PROVIDER -> {
                Optional<NotificationProvider> saved = notificationProviderRepository.findByApplicationId(applicationId).stream()
                        .filter(provider -> id.equals(provider.getDataSourceName())).findFirst();
                ObjectNode node = root(metadata, applicationId, saved.map(NotificationProvider::getId).orElse(null));
                node.set("options", TextNode.valueOf(optionsWithSecrets(metadata.get("options"), saved.map(NotificationProvider::getOptions).orElse(null))));
                notificationProviderRepository.save(convert(node, NotificationProvider.class));
                context.warnings.add("알림 제공자 '" + id + "' 가 바뀌었습니다. [알림 제공자] 화면에서 비밀값을 확인하고 새로고침하세요.");
            }
            case COMMON_CODE -> saveCommonCode(applicationId, id, metadata);
            case QUERY -> {
                List<QueryParam> params = convertList(metadata.get("params"), QueryParam.class);
                Optional<Query> saved = queryRepository.findByApplicationIdAndQueryName(applicationId, id);
                if(saved.isPresent()) {
                    saved.get().update(id, text(metadata, "displayName"), text(metadata, "dataSourceName"), nullToEmpty(document.body()), params);
                    queryRepository.save(saved.get());
                }
                else {
                    queryRepository.save(Query.builder()
                            .applicationId(applicationId)
                            .queryName(id)
                            .displayName(text(metadata, "displayName"))
                            .dataSourceName(text(metadata, "dataSourceName"))
                            .query(nullToEmpty(document.body()))
                            .queryParams(params)
                            .build());
                }
            }
            case ENTITY -> {
                List<EntityColumn> columns = convertList(metadata.get("columns"), EntityColumn.class);
                Optional<Entity> saved = entityRepository.findByApplicationIdAndEntityName(applicationId, id);
                if(saved.isPresent()) {
                    saved.get().update(id, text(metadata, "displayName"), text(metadata, "dataSourceName"), text(metadata, "tableName"), columns);
                    entityRepository.save(saved.get());
                }
                else {
                    entityRepository.save(Entity.builder()
                            .applicationId(applicationId)
                            .entityName(id)
                            .displayName(text(metadata, "displayName"))
                            .dataSourceName(text(metadata, "dataSourceName"))
                            .tableName(text(metadata, "tableName"))
                            .entityColumns(columns)
                            .build());
                }
            }
            case REST_CLIENT -> {
                List<RestClientQueryParam> queryParams = convertList(metadata.get("queryParams"), RestClientQueryParam.class);
                List<RestClientHeader> headers = convertList(metadata.get("headers"), RestClientHeader.class);
                List<RestClientBody> body = convertList(metadata.get("body"), RestClientBody.class);
                ObjectNode node = root(metadata, applicationId, null);
                node.remove(List.of("queryParams", "headers", "body"));
                RestClient values = convert(node, RestClient.class);

                Optional<RestClient> saved = restClientRepository.findByApplicationIdAndClientName(applicationId, id);
                if(saved.isPresent()) {
                    saved.get().update(id, values.getDisplayName(), values.getDataSourceName(), values.getMethod(), values.getPath(),
                            values.getContentType(), values.getBodyMessageFormat(), queryParams, headers, body);
                    restClientRepository.save(saved.get());
                }
                else {
                    restClientRepository.save(RestClient.builder()
                            .applicationId(applicationId)
                            .clientName(id)
                            .displayName(values.getDisplayName())
                            .dataSourceName(values.getDataSourceName())
                            .method(values.getMethod())
                            .path(values.getPath())
                            .contentType(values.getContentType())
                            .bodyMessageFormat(values.getBodyMessageFormat())
                            .queryParams(queryParams)
                            .headers(headers)
                            .body(body)
                            .build());
                }
            }
            case NOTIFICATION -> {
                Long savedId = notificationRepository.findByApplicationIdAndNotificationName(applicationId, id).map(Notification::getId).orElse(null);
                notificationRepository.save(convert(root(metadata, applicationId, savedId), Notification.class));
            }
            case MESSAGE_PROCESSOR -> {
                Long savedId = messageProcessorRepository.findByApplicationIdAndProcessorName(applicationId, id).map(MessageProcessor::getId).orElse(null);
                ObjectNode node = root(metadata, applicationId, savedId);
                node.put("script", nullToEmpty(document.body()));
                messageProcessorRepository.save(convert(node, MessageProcessor.class));
            }
            case WORKFLOW -> saveWorkflow(applicationId, id, metadata);
            case REST_API -> saveRestApi(applicationId, id, metadata);
            case SCHEDULER -> saveSchedulerJob(context, id, metadata);
            case VIEW_OBJECT -> saveViewObject(context, document);
            case SITE -> {
                Long savedId = layoutRepository.findByApplicationId(applicationId).map(Layout::getId).orElse(null);
                layoutRepository.save(convert(root(metadata, applicationId, savedId), Layout.class));
            }
            default -> throw new IllegalStateException("지원하지 않는 DSL 폴더입니다. " + change.folder());
        }
    }

    private void saveCommonCode(String applicationId, String id, JsonNode metadata) {
        Long savedId = commonCodeKindRepository.findByApplicationIdAndCode(applicationId, id).map(CommonCodeKind::getId).orElse(null);
        ObjectNode node = root(metadata, applicationId, savedId);
        node.remove("codes");
        CommonCodeKind codeKind = commonCodeKindRepository.save(convert(node, CommonCodeKind.class));

        commonCodeRepository.deleteAll(commonCodeRepository.findByCodeKind(codeKind));
        commonCodeRepository.flush();
        for(JsonNode code : iterable(metadata.get("codes"))) {
            commonCodeRepository.save(CommonCode.builder()
                    .code(text(code, "code"))
                    .name(text(code, "name"))
                    .orderNum(integer(code, "orderNum"))
                    .codeKind(codeKind)
                    .build());
        }
    }

    private void saveWorkflow(String applicationId, String id, JsonNode metadata) {
        Optional<Workflow> saved = workflowRepository.findByApplicationIdAndWorkflowCode(applicationId, id);
        Workflow workflow;
        if(saved.isPresent()) {
            workflow = saved.get();
            workflow.update(text(metadata, "displayName"), text(metadata, "note"), bool(metadata, "useAuthValidation"));
            workflow = workflowRepository.save(workflow);
        }
        else {
            workflow = workflowRepository.save(convert(root(metadata, applicationId, null), Workflow.class));
        }

        Long workflowId = workflow.getId();
        workflowNodeRepository.deleteByWorkflowId(workflowId);
        workflowEdgeRepository.deleteByWorkflowId(workflowId);
        workflowConditionRepository.deleteByWorkflowId(workflowId);
        workflowErrorResponseRepository.deleteByWorkflowId(workflowId);
        workflowPermissionRepository.deleteByWorkflowId(workflowId);
        workflowIpGroupRepository.deleteByWorkflowId(workflowId);
        workflowDomainRepository.deleteByWorkflowId(workflowId);
        // 지운 행을 먼저 DB 에 반영해야 같은 키(노드ID 등)로 다시 넣을 때 유니크 제약에 걸리지 않는다 (Hibernate 는 insert 를 delete 보다 먼저 flush 한다)
        entityManager.flush();

        for(JsonNode node : iterable(metadata.get("nodes"))) {
            workflowNodeRepository.save(convert(withWorkflowId(node, workflowId), WorkflowNode.class));
        }
        for(JsonNode edge : iterable(metadata.get("edges"))) {
            workflowEdgeRepository.save(convert(withWorkflowId(edge, workflowId), WorkflowEdge.class));
        }
        for(JsonNode condition : iterable(metadata.get("conditions"))) {
            workflowConditionRepository.save(convert(withWorkflowId(condition, workflowId), WorkflowCondition.class));
        }
        for(JsonNode error : iterable(metadata.get("errorResponses"))) {
            workflowErrorResponseRepository.save(convert(withWorkflowId(error, workflowId), WorkflowErrorResponse.class));
        }
        for(String permissionCode : texts(metadata.get("permissions"))) {
            workflowPermissionRepository.save(WorkflowPermission.builder().permissionCode(permissionCode).workflow(workflow).build());
        }
        for(String ipGroupCode : texts(metadata.get("ipGroups"))) {
            workflowIpGroupRepository.save(WorkflowIpGroup.builder().ipGroupCode(ipGroupCode).workflow(workflow).build());
        }
        for(String domain : texts(metadata.get("domains"))) {
            workflowDomainRepository.save(WorkflowDomain.builder().domain(domain.trim().toLowerCase(Locale.ROOT)).workflow(workflow).build());
        }
    }

    // REST API 는 콘솔 저장과 같은 검증(경로, 본문, 파라미터 트리, 메소드+경로 중복)을 거치도록 RestApiService 로 저장한다
    @SuppressWarnings("unchecked")
    private void saveRestApi(String applicationId, String id, JsonNode metadata) {
        Map<String, Object> restApi = ENTITY_MAPPER.convertValue(metadata, Map.class);
        Object parameters = restApi.remove("parameters");
        restApiRepository.findByApplicationIdAndApiCode(applicationId, id)
                .ifPresent(saved -> restApi.put("id", String.valueOf(saved.getId())));

        Map<String, Object> params = new HashMap<>();
        params.put("restApi", restApi);
        params.put("parameters", parameters == null ? List.of() : parameters);

        try {
            restApiService.save(applicationId, params);
        }
        catch (RestApiValidationException e) {
            throw DeployException.badRequest("[RestApi " + id + "] " + e.getMessage());
        }
    }

    private void saveSchedulerJob(Context context, String id, JsonNode metadata) {
        Long savedId = schedulerJobRepository.findByJobCode(id)
                .map(job -> {
                    if(!context.applicationId.equals(job.getApplicationId())) {
                        throw DeployException.badRequest("스케줄러 작업코드 '" + id + "' 를 다른 애플리케이션(" + job.getApplicationId() + ")이 쓰고 있습니다.");
                    }
                    return job.getId();
                })
                .orElse(null);

        ObjectNode node = root(metadata, context.applicationId, savedId);
        node.remove("workflows");
        SchedulerJob job = schedulerJobRepository.save(convert(node, SchedulerJob.class));

        schedulerJobWorkflowRepository.deleteBySchedulerJobId(job.getId());
        entityManager.flush();
        for(JsonNode jobWorkflow : iterable(metadata.get("workflows"))) {
            String workflowCode = text(jobWorkflow, "workflowCode");
            Workflow workflow = workflowCode == null ? null : workflowRepository.findByApplicationIdAndWorkflowCode(context.applicationId, workflowCode)
                    .orElseThrow(() -> new IllegalArgumentException("실행할 워크플로우가 없습니다. [" + workflowCode + "]"));
            schedulerJobWorkflowRepository.save(SchedulerJobWorkflow.builder()
                    .schedulerJob(job)
                    .workflow(workflow)
                    .requestMessageJson(jsonText(jobWorkflow.get("requestMessage")))
                    .orderNum(integer(jobWorkflow, "orderNum"))
                    .build());
        }

        context.schedulerJobs.add(job);
    }

    private void saveViewObject(Context context, DslDocument document) {
        String applicationId = context.applicationId;
        String objectCode = document.id();
        JsonNode metadata = document.metadata();

        ObjectType type = enumOf(ObjectType.class, text(metadata, "type"));
        if(type == null) {
            type = ObjectType.VIEW_BUILDER;
        }

        Optional<ViewObject> saved = viewObjectRepository.findByApplicationIdAndObjectCode(applicationId, objectCode);
        if(saved.isPresent()) {
            ViewObject viewObject = saved.get();
            if(viewObject.getType() != null && viewObject.getType() != type) {
                throw DeployException.badRequest("화면 '" + objectCode + "' 의 유형이 다릅니다 (이 환경: " + viewObject.getType() + ", 저장소: " + type + ").");
            }
            viewObject.update(text(metadata, "objectName"), bool(metadata, "useAuthValidation"),
                    bool(metadata, "usePermissionValidation"), bool(metadata, "useToolbar"));
            viewObjectRepository.save(viewObject);
        }
        else {
            ObjectNode node = root(metadata, applicationId, null);
            node.remove(List.of("content", "codes"));
            node.put("type", type.name());
            viewObjectRepository.save(convert(node, ViewObject.class));
        }

        // 화면빌더 레이아웃 JSON
        String content = ObjectType.FILE.equals(type) ? null : jsonText(metadata.get("content"));
        Optional<ViewObjectContent> savedContent = viewObjectContentRepository.findByApplicationIdAndObjectCode(applicationId, objectCode);
        if(content == null) {
            savedContent.ifPresent(viewObjectContentRepository::delete);
            entityManager.flush();
        }
        else if(savedContent.isPresent()) {
            savedContent.get().update(content);
            viewObjectContentRepository.save(savedContent.get());
        }
        else {
            viewObjectContentRepository.save(ViewObjectContent.builder().applicationId(applicationId).objectCode(objectCode).content(content).build());
        }

        // 코드
        viewCodeRepository.deleteAll(viewCodeRepository.findByApplicationIdAndObjectCode(applicationId, objectCode));
        entityManager.flush();
        for(JsonNode code : iterable(metadata.get("codes"))) {
            viewCodeRepository.save(ViewCode.builder()
                    .applicationId(applicationId)
                    .objectCode(objectCode)
                    .name(text(code, "name"))
                    .target(text(code, "target"))
                    .type(text(code, "type"))
                    .build());
        }

        // 액션
        viewActionRepository.deleteAll(viewActionRepository.findByApplicationIdAndObjectCode(applicationId, objectCode));
        entityManager.flush();
        for(ActionDocument action : document.actions()) {
            JsonNode actionNode = action.metadata();
            viewActionRepository.save(ViewAction.builder()
                    .applicationId(applicationId)
                    .objectCode(objectCode)
                    .actionName(action.actionName())
                    .displayName(text(actionNode, "displayName"))
                    .type(text(actionNode, "type"))
                    .argsName(text(actionNode, "argsName"))
                    .contents(jsonText(actionNode.get("contents")))
                    .script(action.script())
                    .build());
        }

        // FILE 화면의 HTML 소스는 커밋 뒤에 디스크에 쓴다
        if(ObjectType.FILE.equals(type) && document.page() != null) {
            context.viewSourceWrites.put(objectCode, document.page());
        }
    }

    // 메뉴는 상위 메뉴(새로 만드는 메뉴일 수 있다)를 가리키므로 먼저 모두 저장한 뒤 상위 메뉴를 잇는다
    private void saveMenus(Context context, List<Change> changes) {
        String applicationId = context.applicationId;
        Map<String, Menu> menus = new LinkedHashMap<>();

        for(Change change : changes) {
            JsonNode metadata = change.document().metadata();
            ViewObject viewObject = viewObjectOf(applicationId, text(metadata, "objectCode"));
            Optional<Menu> saved = menuRepository.findByApplicationIdAndMenuCd(applicationId, change.id());

            Menu menu;
            if(saved.isPresent()) {
                menu = saved.get();
                menu.update(change.id(), text(metadata, "menuNm"), integer(metadata, "orderNum"), text(metadata, "icon"), menu.getParentMenu(), viewObject);
            }
            else {
                menu = Menu.builder()
                        .applicationId(applicationId)
                        .menuCd(change.id())
                        .menuNm(text(metadata, "menuNm"))
                        .orderNum(integer(metadata, "orderNum"))
                        .icon(text(metadata, "icon"))
                        .viewObject(viewObject)
                        .children(new ArrayList<>())
                        .build();
            }
            menus.put(change.id(), menuRepository.save(menu));
        }
        menuRepository.flush();

        for(Change change : changes) {
            JsonNode metadata = change.document().metadata();
            Menu menu = menus.get(change.id());
            String parentMenuCd = text(metadata, "parentMenuCd");
            Menu parent = parentMenuCd == null ? null : menuRepository.findByApplicationIdAndMenuCd(applicationId, parentMenuCd)
                    .orElseThrow(() -> new IllegalArgumentException("상위 메뉴가 없습니다. [" + parentMenuCd + "]"));
            menu.update(menu.getMenuCd(), menu.getMenuNm(), menu.getOrderNum(), menu.getIcon(), parent, menu.getViewObject());

            menuPermissionRepository.deleteAll(menuPermissionRepository.findByMenu(menu));
            entityManager.flush();
            for(String permissionCode : texts(metadata.get("permissions"))) {
                menuPermissionRepository.save(MenuPermission.builder().permissionCode(permissionCode).menu(menu).build());
            }
        }
    }

    // ================= 삭제 =================

    private void delete(Context context, Change change) {
        String applicationId = context.applicationId;
        String id = change.id();

        switch (change.folder()) {
            case DATABASE -> databaseRepository.findByApplicationId(applicationId).stream()
                    .filter(meta -> id.equals(meta.getDataSourceName())).forEach(databaseRepository::delete);
            case REST_SERVER -> restServerRepository.findByApplicationId(applicationId).stream()
                    .filter(server -> id.equals(server.getDataSourceName())).forEach(restServerRepository::delete);
            case NOTIFICATION_PROVIDER -> notificationProviderRepository.findByApplicationId(applicationId).stream()
                    .filter(provider -> id.equals(provider.getDataSourceName())).forEach(notificationProviderRepository::delete);
            case COMMON_CODE -> commonCodeKindRepository.findByApplicationIdAndCode(applicationId, id).ifPresent(codeKind -> {
                commonCodeRepository.deleteAll(commonCodeRepository.findByCodeKind(codeKind));
                commonCodeKindRepository.delete(codeKind);
            });
            case QUERY -> queryRepository.findByApplicationIdAndQueryName(applicationId, id).ifPresent(queryRepository::delete);
            case ENTITY -> entityRepository.findByApplicationIdAndEntityName(applicationId, id).ifPresent(entityRepository::delete);
            case REST_CLIENT -> restClientRepository.findByApplicationIdAndClientName(applicationId, id).ifPresent(restClientRepository::delete);
            case NOTIFICATION -> notificationRepository.findByApplicationIdAndNotificationName(applicationId, id).ifPresent(notificationRepository::delete);
            case MESSAGE_PROCESSOR -> messageProcessorRepository.findByApplicationIdAndProcessorName(applicationId, id).ifPresent(messageProcessorRepository::delete);
            case WORKFLOW -> workflowRepository.findByApplicationIdAndWorkflowCode(applicationId, id).ifPresent(workflow -> {
                Long workflowId = workflow.getId();
                workflowNodeRepository.deleteByWorkflowId(workflowId);
                workflowEdgeRepository.deleteByWorkflowId(workflowId);
                workflowConditionRepository.deleteByWorkflowId(workflowId);
                workflowErrorResponseRepository.deleteByWorkflowId(workflowId);
                workflowPermissionRepository.deleteByWorkflowId(workflowId);
                workflowIpGroupRepository.deleteByWorkflowId(workflowId);
                workflowDomainRepository.deleteByWorkflowId(workflowId);
                workflowRepository.delete(workflow);
            });
            case SCHEDULER -> schedulerJobRepository.findByJobCode(id)
                    .filter(job -> applicationId.equals(job.getApplicationId()))
                    .ifPresent(job -> {
                        schedulerJobWorkflowRepository.deleteBySchedulerJobId(job.getId());
                        schedulerJobHistoryRepository.deleteBySchedulerJobId(job.getId());
                        schedulerJobRepository.delete(job);
                        context.unscheduleJobCodes.add(id);
                    });
            case VIEW_OBJECT -> viewObjectRepository.findByApplicationIdAndObjectCode(applicationId, id).ifPresent(viewObject -> {
                viewObjectContentRepository.findByApplicationIdAndObjectCode(applicationId, id).ifPresent(viewObjectContentRepository::delete);
                viewActionRepository.deleteAll(viewActionRepository.findByApplicationIdAndObjectCode(applicationId, id));
                viewCodeRepository.deleteAll(viewCodeRepository.findByApplicationIdAndObjectCode(applicationId, id));
                viewObjectRepository.delete(viewObject);
                if(ObjectType.FILE.equals(viewObject.getType()) && ViewObject.isValidCode(id)) {
                    context.viewSourceDeletes.add(id);
                }
            });
            case SITE -> layoutRepository.findByApplicationId(applicationId).ifPresent(layoutRepository::delete);
            default -> throw new IllegalStateException("지원하지 않는 DSL 폴더입니다. " + change.folder());
        }
        entityManager.flush();
    }

    private void deleteRestApi(Context context, String id) {
        restApiRepository.findByApplicationIdAndApiCode(context.applicationId, id)
                .map(RestApi::getId)
                .ifPresent(restApiId -> restApiService.delete(context.applicationId, restApiId));
        entityManager.flush();
    }

    // 지울 메뉴의 메뉴접근권한을 먼저 지우고, 깊은 메뉴부터 지운다 (하위 메뉴는 children cascade 로도 지워진다)
    private void deleteMenus(Context context, List<String> menuCds) {
        List<Menu> targets = menuCds.stream()
                .map(menuCd -> menuRepository.findByApplicationIdAndMenuCd(context.applicationId, menuCd))
                .flatMap(Optional::stream)
                .toList();

        for(Menu menu : targets) {
            menuPermissionRepository.deleteAll(menuPermissionRepository.findByMenu(menu));
        }
        menuPermissionRepository.flush();

        List<Menu> deepestFirst = new ArrayList<>(targets);
        deepestFirst.sort(Comparator.comparingInt(DslImporter::depthOf).reversed());
        for(Menu menu : deepestFirst) {
            Menu current = entityManager.find(Menu.class, menu.getId());
            if(current == null) {
                continue;
            }
            if(current.getParentMenu() != null) {
                current.getParentMenu().getChildren().remove(current);
            }
            menuRepository.delete(current);
            menuRepository.flush();
        }
    }

    private static int depthOf(Menu menu) {
        int depth = 0;
        for(Menu parent = menu.getParentMenu(); parent != null && depth < 100; parent = parent.getParentMenu()) {
            depth++;
        }
        return depth;
    }

    // ================= util =================

    private ViewObject viewObjectOf(String applicationId, String objectCode) {
        if(objectCode == null) {
            return null;
        }
        return viewObjectRepository.findByApplicationIdAndObjectCode(applicationId, objectCode)
                .orElseThrow(() -> new IllegalArgumentException("메뉴가 가리키는 화면이 없습니다. [" + objectCode + "]"));
    }

    // 엔티티로 바꿀 최상위 JSON: kind/version 을 빼고 이 환경의 applicationId, DB id 를 넣는다
    private static ObjectNode root(JsonNode metadata, String applicationId, Long id) {
        ObjectNode node = ((ObjectNode) metadata).deepCopy();
        node.remove(List.of("kind", "version"));
        node.put("applicationId", applicationId);
        if(id == null) {
            node.remove("id");
        }
        else {
            node.put("id", id);
        }
        return node;
    }

    private static ObjectNode withWorkflowId(JsonNode node, Long workflowId) {
        ObjectNode copy = ((ObjectNode) node).deepCopy();
        copy.put("workflowId", workflowId);
        return copy;
    }

    private static <T> T convert(JsonNode node, Class<T> type) {
        try {
            return ENTITY_MAPPER.treeToValue(node, type);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("DSL 값을 읽지 못했습니다. " + e.getMessage(), e);
        }
    }

    private static <T> List<T> convertList(JsonNode array, Class<T> type) {
        List<T> list = new ArrayList<>();
        for(JsonNode node : iterable(array)) {
            list.add(convert(node, type));
        }
        return list;
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        return node == null || !node.isArray() ? List.of() : node;
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        for(JsonNode node : iterable(array)) {
            if(node.isTextual() && !node.asText().isBlank()) {
                values.add(node.asText());
            }
        }
        return values;
    }

    static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if(value == null || value.isNull()) {
            return null;
        }
        if(value.isNumber()) {
            return value.intValue();
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : Integer.valueOf(text);
    }

    private static Boolean bool(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if(value == null || value.isNull()) {
            return null;
        }
        return value.isBoolean() ? value.booleanValue() : Boolean.valueOf(value.asText());
    }

    // DslExporter.jsonOrText 의 반대: JSON 으로 풀어둔 값은 다시 문자열로, 문자열은 그대로
    private static String jsonText(JsonNode node) {
        if(node == null || node.isNull()) {
            return null;
        }
        return node.isContainerNode() ? node.toString() : node.asText();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String value) {
        if(value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    // DSL 에서 빠진 비밀값 키를 이 환경의 옵션에서 가져와 넣는다
    private static String optionsWithSecrets(JsonNode options, String savedOptions) {
        if(options == null || options.isNull()) {
            return savedOptions;
        }
        if(!options.isObject()) {
            return options.asText();
        }

        ObjectNode merged = ((ObjectNode) options).deepCopy();
        if(savedOptions != null && !savedOptions.isBlank()) {
            try {
                JsonNode saved = DslJson.read(savedOptions);
                if(saved != null && saved.isObject()) {
                    saved.fields().forEachRemaining(entry -> {
                        if(SECRET_OPTION_KEY.matcher(entry.getKey()).matches() && !merged.has(entry.getKey())) {
                            merged.set(entry.getKey(), entry.getValue());
                        }
                    });
                }
            }
            catch (Exception ignored) {
                // 저장된 옵션이 JSON 이 아니면 가져올 비밀값이 없다
            }
        }
        return merged.toString();
    }

    private static class Context {
        private final String applicationId;
        private int created;
        private int updated;
        private int deleted;
        private final Map<String, String> viewSourceWrites = new LinkedHashMap<>();
        private final List<String> viewSourceDeletes = new ArrayList<>();
        private final List<SchedulerJob> schedulerJobs = new ArrayList<>();
        private final List<String> unscheduleJobCodes = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        private Context(String applicationId) {
            this.applicationId = applicationId;
        }

        private PostActions postActions() {
            return new PostActions(viewSourceWrites, viewSourceDeletes, schedulerJobs, unscheduleJobCodes, warnings);
        }
    }
}
