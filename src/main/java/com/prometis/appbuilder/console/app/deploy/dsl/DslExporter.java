package com.prometis.appbuilder.console.app.deploy.dsl;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.prometis.appbuilder.app.image.ImageStorage;
import com.prometis.appbuilder.app.image.ImageStorageRepository;
import com.prometis.appbuilder.app.image.StorageType;
import com.prometis.appbuilder.app.image.StorageTypeRepository;
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
import com.prometis.appbuilder.app.restapi.RestApiParameter;
import com.prometis.appbuilder.app.restapi.RestApiParameterRepository;
import com.prometis.appbuilder.app.restapi.RestApiRepository;
import com.prometis.appbuilder.app.restclient.RestClient;
import com.prometis.appbuilder.app.restclient.RestClientBody;
import com.prometis.appbuilder.app.restclient.RestClientHeader;
import com.prometis.appbuilder.app.restclient.RestClientQueryParam;
import com.prometis.appbuilder.app.restclient.RestClientRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerJobRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerJobWorkflowRepository;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJob;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJobWorkflow;
import com.prometis.appbuilder.app.security.company.Company;
import com.prometis.appbuilder.app.security.company.CompanyRepository;
import com.prometis.appbuilder.app.view.LayoutRepository;
import com.prometis.appbuilder.app.view.MenuPermission;
import com.prometis.appbuilder.app.view.MenuPermissionRepository;
import com.prometis.appbuilder.app.view.MenuRepository;
import com.prometis.appbuilder.app.view.ViewActionRepository;
import com.prometis.appbuilder.app.view.ViewCodeRepository;
import com.prometis.appbuilder.app.view.ViewObjectContentRepository;
import com.prometis.appbuilder.app.view.ViewObjectRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 애플리케이션의 메타데이터를 DSL 파일(JSON)로 만든다.
 *
 * - DB id, applicationId 처럼 환경마다 달라지는 값은 넣지 않는다. 메타데이터끼리는 ID(코드, 이름)로 가리킨다.
 * - 목록은 항상 같은 순서로 쓴다. 같은 메타데이터면 같은 파일이 나와야 저장소와 비교할 때 바뀐 것만 잡힌다.
 * - 비밀번호 같은 민감정보는 저장소에 올리지 않는다. (데이터베이스 password, 알림 제공자 options 의 password/secret/token 류)
 *   배포 환경의 값은 그 환경에서 따로 관리한다.
 */
@Component
@RequiredArgsConstructor
public class DslExporter {
    private static final Pattern SECRET_OPTION_KEY = Pattern.compile("(?i).*(password|secret|token|credential).*");

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
    private final RestApiParameterRepository restApiParameterRepository;

    private final SchedulerJobRepository schedulerJobRepository;
    private final SchedulerJobWorkflowRepository schedulerJobWorkflowRepository;

    private final ViewObjectRepository viewObjectRepository;
    private final ViewObjectContentRepository viewObjectContentRepository;
    private final ViewActionRepository viewActionRepository;
    private final ViewCodeRepository viewCodeRepository;
    private final MenuRepository menuRepository;
    private final MenuPermissionRepository menuPermissionRepository;
    private final LayoutRepository layoutRepository;

    private final CompanyRepository companyRepository;

    private final StorageTypeRepository storageTypeRepository;
    private final ImageStorageRepository imageStorageRepository;
    private final CommonCodeKindRepository commonCodeKindRepository;
    private final CommonCodeRepository commonCodeRepository;

    /**
     * 애플리케이션의 DSL 파일 전체. 경로 순으로 정렬해서 돌려준다.
     */
    @Transactional(readOnly = true)
    public List<DslFile> export(String applicationId) {
        List<DslFile> files = new ArrayList<>();

        databaseRepository.findByApplicationId(applicationId)
                .forEach(meta -> files.add(file(applicationId, DslFolder.DATABASE, meta.getDataSourceName(), database(meta))));
        restServerRepository.findByApplicationId(applicationId)
                .forEach(server -> files.add(file(applicationId, DslFolder.REST_SERVER, server.getDataSourceName(), restServer(server))));
        notificationProviderRepository.findByApplicationId(applicationId)
                .forEach(provider -> files.add(file(applicationId, DslFolder.NOTIFICATION_PROVIDER, provider.getDataSourceName(), notificationProvider(provider))));

        workflowRepository.findByApplicationId(applicationId)
                .forEach(workflow -> files.add(file(applicationId, DslFolder.WORKFLOW, workflow.getWorkflowCode(), workflow(workflow))));
        queryRepository.findByApplicationId(applicationId)
                .forEach(query -> {
                    files.add(file(applicationId, DslFolder.QUERY, query.getQueryName(), query(query)));
                    files.add(sqlFile(applicationId, DslFolder.QUERY, query.getQueryName(), query.getQuery()));
                });
        entityRepository.findByApplicationId(applicationId)
                .forEach(entity -> files.add(file(applicationId, DslFolder.ENTITY, entity.getEntityName(), entity(entity))));
        restClientRepository.findByApplicationId(applicationId)
                .forEach(client -> files.add(file(applicationId, DslFolder.REST_CLIENT, client.getClientName(), restClient(client))));
        notificationRepository.findByApplicationId(applicationId)
                .forEach(notification -> files.add(file(applicationId, DslFolder.NOTIFICATION, notification.getNotificationName(), notification(notification))));
        messageProcessorRepository.findByApplicationId(applicationId)
                .forEach(processor -> files.add(file(applicationId, DslFolder.MESSAGE_PROCESSOR, processor.getProcessorName(), messageProcessor(processor))));

        restApiRepository.findByApplicationId(applicationId)
                .forEach(restApi -> files.add(file(applicationId, DslFolder.REST_API, restApi.getApiCode(), restApi(restApi))));

        schedulerJobRepository.findByApplicationId(applicationId)
                .forEach(job -> files.add(file(applicationId, DslFolder.SCHEDULER, job.getJobCode(), schedulerJob(job))));

        exportViews(applicationId, files);

        companyRepository.findOrderedByApplicationId(applicationId)
                .forEach(company -> files.add(file(applicationId, DslFolder.COMPANY, company.getCompanyCode(), company(company))));

        storageTypeRepository.findByApplicationIdOrderByStorageTypeAsc(applicationId)
                .forEach(storageType -> files.add(file(applicationId, DslFolder.IMAGE_STORAGE_TYPE, storageType.getStorageType(), imageStorageType(applicationId, storageType))));
        commonCodeKindRepository.findByApplicationId(applicationId)
                .forEach(codeKind -> files.add(file(applicationId, DslFolder.COMMON_CODE, codeKind.getCode(), commonCodeKind(codeKind))));

        files.sort(Comparator.comparing(DslFile::path));
        return files;
    }

    private void exportViews(String applicationId, List<DslFile> files) {
        Map<String, ViewObjectContent> contents = indexBy(viewObjectContentRepository.findByApplicationId(applicationId), ViewObjectContent::getObjectCode);
        Map<String, List<ViewAction>> actions = groupBy(viewActionRepository.findByApplicationId(applicationId), ViewAction::getObjectCode);
        Map<String, List<ViewCode>> codes = groupBy(viewCodeRepository.findByApplicationId(applicationId), ViewCode::getObjectCode);

        for(ViewObject viewObject : viewObjectRepository.findByApplicationId(applicationId)) {
            String objectCode = viewObject.getObjectCode();
            files.add(file(applicationId, DslFolder.VIEW_OBJECT, objectCode, viewObject(
                    viewObject,
                    contents.get(objectCode),
                    codes.getOrDefault(objectCode, List.of()))));

            // 액션은 {objectCode}/actions/{actionName}/metadata.json, 스크립트가 있으면 그 옆의 script.js 로 뗀다
            for(ViewAction action : actions.getOrDefault(objectCode, List.of())) {
                String actionName = action.getActionName();
                files.add(new DslFile(DslFolder.VIEW_OBJECT.actionFilePath(applicationId, objectCode, actionName),
                        DslFolder.VIEW_OBJECT, objectCode, DslJson.write(viewAction(action))));

                if(action.getScript() != null && !action.getScript().isBlank()) {
                    files.add(new DslFile(DslFolder.VIEW_OBJECT.actionScriptFilePath(applicationId, objectCode, actionName),
                            DslFolder.VIEW_OBJECT, objectCode, action.getScript()));
                }
            }
        }

        menuRepository.findByApplicationId(applicationId)
                .forEach(menu -> files.add(file(applicationId, DslFolder.MENU, menu.getMenuCd(), menu(menu))));

        layoutRepository.findByApplicationId(applicationId)
                .ifPresent(layout -> files.add(file(applicationId, DslFolder.SITE, DslFolder.FIXED_SITE_ID, site(layout))));
    }

    private static DslFile file(String applicationId, DslFolder folder, String id, Map<String, Object> document) {
        return new DslFile(folder.filePath(applicationId, id), folder, id, DslJson.write(document));
    }

    // SQL 본문은 {ID}/metadata.json 옆의 {ID}/query.sql 로 뗀다. 저장된 글자 그대로 쓴다(줄바꿈도 손대지 않는다)
    private static DslFile sqlFile(String applicationId, DslFolder folder, String id, String sql) {
        return new DslFile(folder.sqlFilePath(applicationId, id), folder, id, sql == null ? "" : sql);
    }

    // ---- datasource ----

    private Map<String, Object> database(DataSourceDatabaseMeta meta) {
        Map<String, Object> d = DslJson.document(DslFolder.DATABASE);
        d.put("dataSourceName", meta.getDataSourceName());
        d.put("displayName", meta.getDisplayName());
        d.put("note", meta.getNote());
        d.put("databaseKind", meta.getDatabaseKind());
        d.put("url", meta.getUrl());
        d.put("username", meta.getUsername());
        d.put("maximumPoolSize", meta.getMaximumPoolSize());
        d.put("minimumIdle", meta.getMinimumIdle());
        d.put("connectionTimeout", meta.getConnectionTimeout());
        d.put("validationTimeout", meta.getValidationTimeout());
        return d;
    }

    private Map<String, Object> restServer(DataSourceRestServer server) {
        Map<String, Object> d = DslJson.document(DslFolder.REST_SERVER);
        d.put("dataSourceName", server.getDataSourceName());
        d.put("displayName", server.getDisplayName());
        d.put("note", server.getNote());
        d.put("baseUrl", server.getBaseUrl());
        d.put("connectTimeout", server.getConnectTimeout());
        d.put("connectRequestTimeout", server.getConnectRequestTimeout());
        d.put("connectionMaxTotal", server.getConnectionMaxTotal());
        d.put("connectionDefaultMaxPerRoute", server.getConnectionDefaultMaxPerRoute());
        return d;
    }

    private Map<String, Object> notificationProvider(NotificationProvider provider) {
        Map<String, Object> d = DslJson.document(DslFolder.NOTIFICATION_PROVIDER);
        d.put("dataSourceName", provider.getDataSourceName());
        d.put("displayName", provider.getDisplayName());
        d.put("note", provider.getNote());
        d.put("type", provider.getType());
        d.put("options", withoutSecrets(DslJson.jsonOrText(provider.getOptions())));
        return d;
    }

    private static Object withoutSecrets(Object options) {
        if(!(options instanceof ObjectNode objectNode)) {
            return options;
        }

        ObjectNode copy = objectNode.deepCopy();
        List<String> secretKeys = new ArrayList<>();
        copy.fieldNames().forEachRemaining(key -> {
            if(SECRET_OPTION_KEY.matcher(key).matches()) {
                secretKeys.add(key);
            }
        });
        copy.remove(secretKeys);
        return sortedFields(copy);
    }

    // 옵션 JSON 은 화면이 넣는 순서대로 저장되므로, 비교가 흔들리지 않게 키 순으로 정렬한다
    private static ObjectNode sortedFields(ObjectNode node) {
        ObjectNode sorted = node.objectNode();
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        Collections.sort(keys);
        keys.forEach(key -> sorted.set(key, node.get(key)));
        return sorted;
    }

    // ---- workflow ----

    private Map<String, Object> workflow(Workflow workflow) {
        Long workflowId = workflow.getId();

        Map<String, Object> d = DslJson.document(DslFolder.WORKFLOW);
        d.put("workflowCode", workflow.getWorkflowCode());
        d.put("displayName", workflow.getDisplayName());
        d.put("note", workflow.getNote());
        d.put("useAuthValidation", workflow.getUseAuthValidation());
        d.put("permissions", sortedTexts(workflowPermissionRepository.findByWorkflow(workflow).stream().map(WorkflowPermission::getPermissionCode).toList()));
        d.put("ipGroups", sortedTexts(workflowIpGroupRepository.findByWorkflow(workflow).stream().map(WorkflowIpGroup::getIpGroupCode).toList()));
        d.put("domains", sortedTexts(workflowDomainRepository.findByWorkflow(workflow).stream().map(WorkflowDomain::getDomain).toList()));

        d.put("nodes", workflowNodeRepository.findByWorkflowId(workflowId).stream()
                .sorted(Comparator.comparing(WorkflowNode::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(WorkflowNode::getNodeId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(node -> {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("nodeId", node.getNodeId());
                    n.put("functionType", node.getFunctionType());
                    n.put("functionName", node.getFunctionName());
                    n.put("errorResolveType", node.getErrorResolveType());
                    n.put("orderNum", node.getOrderNum());
                    n.put("isLogging", node.getIsLogging());
                    n.put("requestMessageId", node.getRequestMessageId());
                    n.put("responseMessageId", node.getResponseMessageId());
                    n.put("positionX", node.getPositionX());
                    n.put("positionY", node.getPositionY());
                    return n;
                })
                .toList());

        d.put("edges", workflowEdgeRepository.findByWorkflowId(workflowId).stream()
                .sorted(Comparator.comparing(WorkflowEdge::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(WorkflowEdge::getSourceNodeId)
                        .thenComparing(WorkflowEdge::getTargetNodeId))
                .map(edge -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("sourceNodeId", edge.getSourceNodeId());
                    e.put("targetNodeId", edge.getTargetNodeId());
                    e.put("branchType", edge.getBranchType());
                    e.put("branchId", edge.getBranchId());
                    e.put("orderNum", edge.getOrderNum());
                    return e;
                })
                .toList());

        d.put("conditions", workflowConditionRepository.findByWorkflowId(workflowId).stream()
                .sorted(Comparator.comparing(WorkflowCondition::getNodeId)
                        .thenComparing(WorkflowCondition::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(WorkflowCondition::getBranchId))
                .map(condition -> {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("nodeId", condition.getNodeId());
                    c.put("branchId", condition.getBranchId());
                    c.put("conditionExpression", condition.getConditionExpression());
                    c.put("orderNum", condition.getOrderNum());
                    return c;
                })
                .toList());

        d.put("errorResponses", workflowErrorResponseRepository.findByWorkflowId(workflowId).stream()
                .sorted(Comparator.comparing(WorkflowErrorResponse::getNodeId))
                .map(error -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("nodeId", error.getNodeId());
                    e.put("status", error.getStatus());
                    e.put("code", error.getCode());
                    e.put("message", error.getMessage());
                    e.put("contents", error.getContents());
                    return e;
                })
                .toList());
        return d;
    }

    private Map<String, Object> query(Query query) {
        Map<String, Object> d = DslJson.document(DslFolder.QUERY);
        d.put("queryName", query.getQueryName());
        d.put("displayName", query.getDisplayName());
        d.put("dataSourceName", query.getDataSourceName());
        // SQL 본문(query)은 같은 폴더의 query.sql 에 있다
        // 파라미터 순서가 인덱스 바인딩 순서다. 저장된 순서(id 순)를 그대로 쓴다
        d.put("params", nullSafe(query.getQueryParams()).stream()
                .sorted(Comparator.comparing(QueryParam::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(param -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("paramName", param.getParamName());
                    p.put("paramType", param.getParamType());
                    p.put("autoValueType", param.getAutoValueType());
                    p.put("inOut", param.getInOut());
                    return p;
                })
                .toList());
        return d;
    }

    private Map<String, Object> entity(Entity entity) {
        Map<String, Object> d = DslJson.document(DslFolder.ENTITY);
        d.put("entityName", entity.getEntityName());
        d.put("displayName", entity.getDisplayName());
        d.put("dataSourceName", entity.getDataSourceName());
        d.put("tableName", entity.getTableName());
        d.put("columns", nullSafe(entity.getEntityColumns()).stream()
                .sorted(Comparator.comparing(EntityColumn::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EntityColumn::getColumnName))
                .map(column -> {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("columnName", column.getColumnName());
                    c.put("displayName", column.getDisplayName());
                    c.put("orderNum", column.getOrderNum());
                    c.put("useKey", column.getUseKey());
                    c.put("columnType", column.getColumnType());
                    c.put("selectWhereType", column.getSelectWhereType());
                    c.put("selectWhereCompareOperator", column.getSelectWhereCompareOperator());
                    c.put("selectOrderByNum", column.getSelectOrderByNum());
                    c.put("selectOrderBySortOrder", column.getSelectOrderBySortOrder());
                    c.put("insertAutoValueType", column.getInsertAutoValueType());
                    c.put("insertAutoValue", column.getInsertAutoValue());
                    c.put("insertNullResolveType", column.getInsertNullResolveType());
                    c.put("updateAutoValueType", column.getUpdateAutoValueType());
                    c.put("updateAutoValue", column.getUpdateAutoValue());
                    c.put("updateNullResolveType", column.getUpdateNullResolveType());
                    c.put("deleteAutoValueType", column.getDeleteAutoValueType());
                    c.put("deleteAutoValue", column.getDeleteAutoValue());
                    return c;
                })
                .toList());
        return d;
    }

    private Map<String, Object> restClient(RestClient client) {
        Map<String, Object> d = DslJson.document(DslFolder.REST_CLIENT);
        d.put("clientName", client.getClientName());
        d.put("displayName", client.getDisplayName());
        d.put("dataSourceName", client.getDataSourceName());
        d.put("method", client.getMethod());
        d.put("path", client.getPath());
        d.put("contentType", client.getContentType());
        d.put("bodyMessageFormat", client.getBodyMessageFormat());
        d.put("queryParams", nullSafe(client.getQueryParams()).stream()
                .sorted(Comparator.comparing(RestClientQueryParam::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(param -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("paramName", param.getParamName());
                    p.put("valueType", param.getValueType());
                    p.put("inputValue", param.getInputValue());
                    return p;
                })
                .toList());
        d.put("headers", nullSafe(client.getHeaders()).stream()
                .sorted(Comparator.comparing(RestClientHeader::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(header -> {
                    Map<String, Object> h = new LinkedHashMap<>();
                    h.put("name", header.getName());
                    h.put("valueType", header.getValueType());
                    h.put("inputValue", header.getInputValue());
                    return h;
                })
                .toList());
        d.put("body", nullSafe(client.getBody()).stream()
                .sorted(Comparator.comparing(RestClientBody::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RestClientBody::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(body -> {
                    Map<String, Object> b = new LinkedHashMap<>();
                    b.put("paramName", body.getParamName());
                    b.put("parentParamName", body.getParentParamName());
                    b.put("dataType", body.getDataType());
                    b.put("valueType", body.getValueType());
                    b.put("inputValue", body.getInputValue());
                    b.put("orderNum", body.getOrderNum());
                    return b;
                })
                .toList());
        return d;
    }

    private Map<String, Object> notification(Notification notification) {
        Map<String, Object> d = DslJson.document(DslFolder.NOTIFICATION);
        d.put("notificationName", notification.getNotificationName());
        d.put("displayName", notification.getDisplayName());
        d.put("dataSourceName", notification.getDataSourceName());
        d.put("enableSend", notification.getEnableSend());
        d.put("toAlias", notification.getToAlias());
        d.put("subject", notification.getSubject());
        d.put("content", notification.getContent());
        return d;
    }

    private Map<String, Object> messageProcessor(MessageProcessor processor) {
        Map<String, Object> d = DslJson.document(DslFolder.MESSAGE_PROCESSOR);
        d.put("processorName", processor.getProcessorName());
        d.put("displayName", processor.getDisplayName());
        d.put("processorType", processor.getProcessorType());
        d.put("script", processor.getScript());
        return d;
    }

    // ---- rest api ----

    private Map<String, Object> restApi(RestApi restApi) {
        Map<String, Object> d = DslJson.document(DslFolder.REST_API);
        d.put("apiCode", restApi.getApiCode());
        d.put("displayName", restApi.getDisplayName());
        d.put("httpMethod", restApi.getHttpMethod());
        d.put("path", restApi.getPath());
        d.put("workflowCode", restApi.getWorkflowCode());
        d.put("bodyDataType", restApi.getBodyDataType());
        d.put("bodyMessageId", restApi.getBodyMessageId());
        d.put("description", restApi.getDescription());
        d.put("enabled", restApi.getEnabled());
        d.put("responseType", restApi.getResponseType());
        d.put("responseMessageId", restApi.getResponseMessageId());
        d.put("responseDataType", restApi.getResponseDataType());
        d.put("fileMessageId", restApi.getFileMessageId());
        d.put("fileContentKey", restApi.getFileContentKey());
        d.put("fileContentEncoding", restApi.getFileContentEncoding());
        d.put("fileNameKey", restApi.getFileNameKey());
        d.put("fileName", restApi.getFileName());
        d.put("fileContentTypeKey", restApi.getFileContentTypeKey());
        d.put("parameters", restApiParameterRepository.findByRestApiIdOrderByOrderNum(restApi.getId()).stream()
                .sorted(Comparator.comparing(RestApiParameter::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RestApiParameter::getParamKey))
                .map(parameter -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("paramIn", parameter.getParamIn());
                    p.put("paramKey", parameter.getParamKey());
                    p.put("parentKey", parameter.getParentKey());
                    p.put("name", parameter.getName());
                    p.put("dataType", parameter.getDataType());
                    p.put("required", parameter.getRequired());
                    p.put("defaultValue", parameter.getDefaultValue());
                    p.put("messageId", parameter.getMessageId());
                    p.put("mappingKey", parameter.getMappingKey());
                    p.put("description", parameter.getDescription());
                    p.put("orderNum", parameter.getOrderNum());
                    return p;
                })
                .toList());
        return d;
    }

    // ---- scheduler ----

    private Map<String, Object> schedulerJob(SchedulerJob job) {
        Map<String, Object> d = DslJson.document(DslFolder.SCHEDULER);
        d.put("jobCode", job.getJobCode());
        d.put("jobName", job.getJobName());
        d.put("cronExpression", job.getCronExpression());
        d.put("description", job.getDescription());
        d.put("enabled", job.getEnabled());
        d.put("workflows", schedulerJobWorkflowRepository.findBySchedulerJobIdOrderByOrderNum(job.getId()).stream()
                .sorted(Comparator.comparing(SchedulerJobWorkflow::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(jobWorkflow -> {
                    Map<String, Object> w = new LinkedHashMap<>();
                    w.put("workflowCode", jobWorkflow.getWorkflow() == null ? null : jobWorkflow.getWorkflow().getWorkflowCode());
                    w.put("orderNum", jobWorkflow.getOrderNum());
                    w.put("requestMessage", DslJson.jsonOrText(jobWorkflow.getRequestMessageJson()));
                    return w;
                })
                .toList());
        return d;
    }

    // ---- view ----

    // 액션(actions)은 같은 폴더의 actions/{actionName}/ 에 있다
    private Map<String, Object> viewObject(ViewObject viewObject, ViewObjectContent content, List<ViewCode> codes) {
        Map<String, Object> d = DslJson.document(DslFolder.VIEW_OBJECT);
        d.put("objectCode", viewObject.getObjectCode());
        d.put("objectName", viewObject.getObjectName());
        d.put("type", viewObject.getType());
        d.put("path", viewObject.getPath());
        d.put("useAuthValidation", viewObject.getUseAuthValidation());
        d.put("usePermissionValidation", viewObject.getUsePermissionValidation());
        d.put("useToolbar", viewObject.getUseToolbar());
        d.put("content", content == null ? null : DslJson.jsonOrText(content.getContent()));
        d.put("codes", codes.stream()
                .sorted(Comparator.comparing(ViewCode::getName))
                .map(code -> {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("name", code.getName());
                    c.put("target", code.getTarget());
                    c.put("type", code.getType());
                    return c;
                })
                .toList());
        return d;
    }

    // 스크립트(script)는 같은 폴더의 script.js 에 있다
    private static Map<String, Object> viewAction(ViewAction action) {
        Map<String, Object> d = DslJson.document(DslFolder.ACTION_KIND);
        d.put("actionName", action.getActionName());
        d.put("displayName", action.getDisplayName());
        d.put("type", action.getType());
        d.put("argsName", action.getArgsName());
        d.put("contents", DslJson.jsonOrText(action.getContents()));
        return d;
    }

    private Map<String, Object> menu(Menu menu) {
        Map<String, Object> d = DslJson.document(DslFolder.MENU);
        d.put("menuCd", menu.getMenuCd());
        d.put("menuNm", menu.getMenuNm());
        d.put("parentMenuCd", menu.getParentMenu() == null ? null : menu.getParentMenu().getMenuCd());
        d.put("orderNum", menu.getOrderNum());
        d.put("icon", menu.getIcon());
        d.put("objectCode", menu.getViewObject() == null ? null : menu.getViewObject().getObjectCode());
        d.put("permissions", sortedTexts(menuPermissionRepository.findByMenu(menu).stream().map(MenuPermission::getPermissionCode).toList()));
        return d;
    }

    private Map<String, Object> site(Layout layout) {
        Map<String, Object> d = DslJson.document(DslFolder.SITE);
        d.put("layoutTitle", layout.getLayoutTitle());
        d.put("homeObjectCode", layout.getHomeObjectCode());
        d.put("useLogo", layout.getUseLogo());
        d.put("logoText", layout.getLogoText());
        d.put("logoBackgroundColor", layout.getLogoBackgroundColor());
        d.put("logoTextColor", layout.getLogoTextColor());
        d.put("logoLink", layout.getLogoLink());
        d.put("logoImg", layout.getLogoImg());
        d.put("faviconPath", layout.getFaviconPath());
        d.put("useCompanySelect", layout.getUseCompanySelect());
        d.put("useProfile", layout.getUseProfile());
        d.put("profileImg", layout.getProfileImg());
        d.put("profileImgSize", layout.getProfileImgSize());
        d.put("loginImgLogoLink", layout.getLoginImgLogoLink());
        return d;
    }

    // ---- security ----

    private Map<String, Object> company(Company company) {
        Map<String, Object> d = DslJson.document(DslFolder.COMPANY);
        d.put("companyCode", company.getCompanyCode());
        d.put("companyName", company.getCompanyName());
        d.put("orderNum", company.getOrderNum());
        return d;
    }

    // ---- setting ----

    private Map<String, Object> imageStorageType(String applicationId, StorageType storageType) {
        Map<String, Object> d = DslJson.document(DslFolder.IMAGE_STORAGE_TYPE);
        d.put("storageType", storageType.getStorageType());
        d.put("displayName", storageType.getDisplayName());
        // 이미지 본문은 base64 로 담는다. 배포 환경에서 같은 링크(/{applicationId}/image/{storageType}/{filename})로 보이게 하려면 파일도 함께 가야 한다
        d.put("images", imageStorageRepository.findByApplicationIdAndStorageTypeOrderByFilenameAsc(applicationId, storageType.getStorageType()).stream()
                .map(image -> {
                    Map<String, Object> i = new LinkedHashMap<>();
                    i.put("filename", image.getFilename());
                    i.put("originalFilename", image.getOriginalFilename());
                    i.put("fileExtension", image.getFileExtension());
                    i.put("fileSize", image.getFileSize());
                    i.put("content", image.getContent() == null ? null : Base64.getEncoder().encodeToString(image.getContent()));
                    return i;
                })
                .toList());
        return d;
    }

    private Map<String, Object> commonCodeKind(CommonCodeKind codeKind) {
        Map<String, Object> d = DslJson.document(DslFolder.COMMON_CODE);
        d.put("code", codeKind.getCode());
        d.put("name", codeKind.getName());
        d.put("codes", commonCodeRepository.findByCodeKind(codeKind).stream()
                .sorted(Comparator.comparing(CommonCode::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CommonCode::getCode))
                .map(code -> {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("code", code.getCode());
                    c.put("name", code.getName());
                    c.put("orderNum", code.getOrderNum());
                    return c;
                })
                .toList());
        return d;
    }

    // ---- util ----

    private static List<String> sortedTexts(List<String> values) {
        return values.stream().filter(Objects::nonNull).distinct().sorted().toList();
    }

    private static <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static <T> Map<String, T> indexBy(List<T> values, Function<T, String> key) {
        Map<String, T> result = new HashMap<>();
        values.forEach(value -> result.put(key.apply(value), value));
        return result;
    }

    private static <T> Map<String, List<T>> groupBy(List<T> values, Function<T, String> key) {
        Map<String, List<T>> result = new HashMap<>();
        values.forEach(value -> result.computeIfAbsent(key.apply(value), k -> new ArrayList<>()).add(value));
        return result;
    }
}
