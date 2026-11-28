package com.prometis.appbuilder.console.app.deploy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prometis.appbuilder.app.code.CommonCode;
import com.prometis.appbuilder.app.code.CommonCodeKind;
import com.prometis.appbuilder.app.code.CommonCodeKindRepository;
import com.prometis.appbuilder.app.code.CommonCodeRepository;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMeta;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMetaRepository;
import com.prometis.appbuilder.app.datasource.database.DatabaseKind;
import com.prometis.appbuilder.app.datasource.notification.NotificationProvider;
import com.prometis.appbuilder.app.datasource.notification.NotificationProviderRepository;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServer;
import com.prometis.appbuilder.app.datasource.restserver.DataSourceRestServerRepository;
import com.prometis.appbuilder.app.entity.Entity;
import com.prometis.appbuilder.app.entity.EntityColumn;
import com.prometis.appbuilder.app.entity.EntityRepository;
import com.prometis.appbuilder.app.entity.code.ColumnType;
import com.prometis.appbuilder.app.executor.notification.NotificationType;
import com.prometis.appbuilder.app.executor.rest.HttpMethod;
import com.prometis.appbuilder.app.message.MessageProcessor;
import com.prometis.appbuilder.app.message.MessageProcessorRepository;
import com.prometis.appbuilder.app.message.code.ProcessorType;
import com.prometis.appbuilder.app.node.WorkflowNode;
import com.prometis.appbuilder.app.node.WorkflowNodeRepository;
import com.prometis.appbuilder.app.node.code.FunctionType;
import com.prometis.appbuilder.app.notification.Notification;
import com.prometis.appbuilder.app.notification.NotificationRepository;
import com.prometis.appbuilder.app.query.Query;
import com.prometis.appbuilder.app.query.QueryParam;
import com.prometis.appbuilder.app.query.QueryRepository;
import com.prometis.appbuilder.app.query.code.AutoValueType;
import com.prometis.appbuilder.app.query.code.InOut;
import com.prometis.appbuilder.app.query.code.ParamType;
import com.prometis.appbuilder.app.restapi.RestApi;
import com.prometis.appbuilder.app.restapi.RestApiParameter;
import com.prometis.appbuilder.app.restapi.RestApiParameterRepository;
import com.prometis.appbuilder.app.restapi.RestApiRepository;
import com.prometis.appbuilder.app.restapi.RestApiService;
import com.prometis.appbuilder.app.restapi.code.HttpMethodType;
import com.prometis.appbuilder.app.restapi.code.ParameterIn;
import com.prometis.appbuilder.app.restapi.code.SchemaDataType;
import com.prometis.appbuilder.app.restapi.code.ResponseType;
import com.prometis.appbuilder.app.restclient.RestClient;
import com.prometis.appbuilder.app.restclient.RestClientHeader;
import com.prometis.appbuilder.app.restclient.RestClientRepository;
import com.prometis.appbuilder.app.restclient.code.ValueType;
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
import com.prometis.appbuilder.app.view.ViewSourceStorage;
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
import com.prometis.appbuilder.app.workflow.WorkflowPermission;
import com.prometis.appbuilder.app.workflow.WorkflowPermissionRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import com.prometis.appbuilder.app.workflow.code.BranchType;
import com.prometis.appbuilder.console.app.deploy.dsl.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DSL 가져오기 왕복 검증: dev 메타데이터 -> DSL -> prod 로 가져오기 -> prod 를 다시 DSL 로 만들면 같은 파일이 나와야 한다.
 * 이어서 저장소 쪽 DSL 을 고쳐(수정/추가/삭제) 다시 가져오면 prod 가 그 DSL 과 같아지는지, 비밀값이 남는지 본다.
 */
@DataJpaTest
@Import({DslExporter.class, DslValidator.class, DslImporter.class, ViewSourceStorage.class, RestApiService.class})
class DslImporterTest {
    private static final String DEV = "dev";
    private static final String PROD = "prod";

    @TempDir
    static Path viewRoot;

    @DynamicPropertySource
    static void viewRootPath(DynamicPropertyRegistry registry) {
        registry.add("platform.view.root-path", () -> viewRoot.toString());
    }

    @Autowired private DslExporter dslExporter;
    @Autowired private DslValidator dslValidator;
    @Autowired private DslImporter dslImporter;
    @Autowired private ViewSourceStorage viewSourceStorage;
    @Autowired private EntityManager entityManager;

    @Autowired private DataSourceDatabaseMetaRepository databaseRepository;
    @Autowired private DataSourceRestServerRepository restServerRepository;
    @Autowired private NotificationProviderRepository notificationProviderRepository;
    @Autowired private CommonCodeKindRepository commonCodeKindRepository;
    @Autowired private CommonCodeRepository commonCodeRepository;
    @Autowired private QueryRepository queryRepository;
    @Autowired private EntityRepository entityRepository;
    @Autowired private RestClientRepository restClientRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private MessageProcessorRepository messageProcessorRepository;
    @Autowired private WorkflowRepository workflowRepository;
    @Autowired private WorkflowNodeRepository workflowNodeRepository;
    @Autowired private WorkflowEdgeRepository workflowEdgeRepository;
    @Autowired private WorkflowConditionRepository workflowConditionRepository;
    @Autowired private WorkflowErrorResponseRepository workflowErrorResponseRepository;
    @Autowired private WorkflowPermissionRepository workflowPermissionRepository;
    @Autowired private WorkflowDomainRepository workflowDomainRepository;
    @Autowired private RestApiRepository restApiRepository;
    @Autowired private RestApiParameterRepository restApiParameterRepository;
    @Autowired private SchedulerJobRepository schedulerJobRepository;
    @Autowired private SchedulerJobWorkflowRepository schedulerJobWorkflowRepository;
    @Autowired private ViewObjectRepository viewObjectRepository;
    @Autowired private ViewObjectContentRepository viewObjectContentRepository;
    @Autowired private ViewActionRepository viewActionRepository;
    @Autowired private ViewCodeRepository viewCodeRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private MenuPermissionRepository menuPermissionRepository;
    @Autowired private LayoutRepository layoutRepository;

    @Test
    void 저장소_DSL을_가져오면_다시_내보낸_DSL이_같다() throws Exception {
        seedDev();
        entityManager.flush();
        entityManager.clear();

        Map<String, String> devFiles = relativeContents(DEV);
        assertFalse(DslValidator.hasError(dslValidator.validate(DEV, withPrefix(DEV, devFiles))), "dev DSL 검증");

        // REST API 코드와 스케줄러 작업코드는 서버 전체에서 하나라서, 같은 DB 의 dev 것을 지우고 prod 로 가져온다
        restApiRepository.findByApplicationId(DEV).forEach(api -> {
            restApiParameterRepository.deleteByRestApiId(api.getId());
            restApiRepository.delete(api);
        });
        schedulerJobRepository.findByApplicationId(DEV).forEach(job -> {
            schedulerJobWorkflowRepository.deleteBySchedulerJobId(job.getId());
            schedulerJobRepository.delete(job);
        });
        entityManager.flush();

        // 1) 빈 prod 로 전부 가져오기
        DslImportPlanner.Plan first = plan(PROD, DEV, devFiles);
        assertTrue(first.changes().stream().allMatch(change -> change.action() == DslImporter.Action.CREATE));
        assertTrue(dslImporter.check(PROD, first.changes()).isEmpty());

        DslImporter.Outcome outcome = dslImporter.apply(PROD, first.changes());
        assertEquals(first.changes().size(), outcome.created());
        assertEquals("<div>파일 화면</div>\r\n", outcome.postActions().viewSourceWrites().get("F001"));
        assertEquals(List.of("JOB_DAILY"), outcome.postActions().schedulerJobs().stream().map(SchedulerJob::getJobCode).toList());
        outcome.postActions().viewSourceWrites().forEach((code, content) -> viewSourceStorage.write(PROD, code, content));
        entityManager.clear();

        assertSameFiles(devFiles, relativeContents(PROD));

        // prod 에만 있는 비밀번호 / 알림 비밀값
        DataSourceDatabaseMeta prodDb = databaseRepository.findByApplicationId(PROD).get(0);
        assertEquals("", prodDb.getPassword());
        entityManager.createQuery("update DataSourceDatabaseMeta d set d.password = 'prod-secret' where d.id = :id")
                .setParameter("id", prodDb.getId()).executeUpdate();
        NotificationProvider prodMail = notificationProviderRepository.findByApplicationId(PROD).get(0);
        entityManager.createQuery("update NotificationProvider n set n.options = :options where n.id = :id")
                .setParameter("options", "{\"host\":\"smtp\",\"password\":\"mail-secret\",\"port\":\"587\"}")
                .setParameter("id", prodMail.getId()).executeUpdate();
        entityManager.clear();

        // 2) 저장소 쪽을 고친다: 수정 / 삭제 / 추가
        Map<String, String> remote = new TreeMap<>(devFiles);
        remote.put("workflow/workflow/W001.json", edit(remote.get("workflow/workflow/W001.json"), node -> node.put("displayName", "바뀐 워크플로우")));
        remote.put("datasource/database/MAIN_DB.json", edit(remote.get("datasource/database/MAIN_DB.json"), node -> node.put("url", "jdbc:mysql://prod-db")));
        remote.put("datasource/notification-provider/MAIL.json", edit(remote.get("datasource/notification-provider/MAIL.json"), node -> ((ObjectNode) node.get("options")).put("host", "smtp2")));
        remote.put("workflow/query/Q001/query.sql", "select 2");
        remote.remove("view/view-object/V001/actions/save/script.js");
        // 하위 메뉴 M002 와 그 화면 V002 를 지우고, 메뉴 M001 을 루트로 올린다
        remote.remove("view/menu/M002.json");
        remove(remote, "view/view-object/V002/");
        remote.put("view/menu/M001.json", edit(remote.get("view/menu/M001.json"), node -> node.putNull("parentMenuCd")));
        // 쿼리 Q003 추가
        remote.put("workflow/query/Q003/metadata.json", edit(remote.get("workflow/query/Q001/metadata.json"), node -> node.put("queryName", "Q003")));
        remote.put("workflow/query/Q003/query.sql", "select 3");
        assertFalse(DslValidator.hasError(dslValidator.validate(DEV, withPrefix(DEV, remote))), "고친 DSL 검증");

        DslImportPlanner.Plan second = plan(PROD, DEV, remote);
        Map<String, DslImporter.Action> actions = new TreeMap<>();
        second.changes().forEach(change -> actions.put(change.folder().kind() + ":" + change.id(), change.action()));
        assertEquals(Map.of(
                "Database:MAIN_DB", DslImporter.Action.UPDATE,
                "NotificationProvider:MAIL", DslImporter.Action.UPDATE,
                "Workflow:W001", DslImporter.Action.UPDATE,
                "Query:Q001", DslImporter.Action.UPDATE,
                "Query:Q003", DslImporter.Action.CREATE,
                "ViewObject:V001", DslImporter.Action.UPDATE,
                "ViewObject:V002", DslImporter.Action.DELETE,
                "Menu:M001", DslImporter.Action.UPDATE,
                "Menu:M002", DslImporter.Action.DELETE
        ), actions);

        DslImporter.Outcome secondOutcome = dslImporter.apply(PROD, second.changes());
        assertEquals(1, secondOutcome.created());
        assertEquals(6, secondOutcome.updated());
        assertEquals(2, secondOutcome.deleted());
        entityManager.clear();

        assertSameFiles(remote, relativeContents(PROD));
        assertEquals("prod-secret", databaseRepository.findByApplicationId(PROD).get(0).getPassword());
        String options = notificationProviderRepository.findByApplicationId(PROD).get(0).getOptions();
        assertTrue(options.contains("mail-secret"), options);
        assertTrue(options.contains("smtp2"), options);

        // 3) 같은 DSL 을 다시 비교하면 바뀐 것이 없다
        assertTrue(plan(PROD, DEV, remote).changes().isEmpty());
    }

    @Test
    void 골라서_배포할때_참조가_깨지면_검증오류이고_같이_고르면_반영된다() throws Exception {
        databaseRepository.save(DataSourceDatabaseMeta.builder()
                .applicationId(DEV).dataSourceName("MAIN_DB").displayName("메인").databaseKind(DatabaseKind.MYSQL)
                .url("jdbc:mysql://dev-db").username("app").password("dev-secret").build());
        queryRepository.save(Query.builder()
                .applicationId(DEV).queryName("Q001").displayName("조회").dataSourceName("MAIN_DB").query("select 1")
                .queryParams(new ArrayList<>()).build());
        Workflow workflow = workflowRepository.save(Workflow.builder().applicationId(DEV).workflowCode("W001").displayName("워크플로우").useAuthValidation(true).build());
        workflowNodeRepository.save(WorkflowNode.builder().workflowId(workflow.getId()).nodeId("n1").functionName("Q001").functionType(FunctionType.SQL).orderNum(1).build());
        commonCodeKindRepository.save(CommonCodeKind.builder().applicationId(DEV).code("STATUS").name("상태").build());
        entityManager.flush();
        entityManager.clear();

        Map<String, DslFile> local = new TreeMap<>();
        for(DslFile file : dslExporter.export(PROD)) {
            local.put(file.path().substring(PROD.length() + 1), file);
        }
        Map<String, String> remote = relativeContents(DEV);
        DslImportPlanner.Plan plan = DslImportPlanner.plan(DEV, local, remote, false);
        assertEquals(4, plan.changes().size());

        // 워크플로우만 고르면 워크플로우가 가리키는 쿼리(Q001)가 prod 에 없어서 오류
        DslImportPlanner.Plan onlyWorkflow = plan.select(List.of(DslImportPlanner.keyOf("Workflow", "W001")));
        assertEquals(1, onlyWorkflow.changes().size());
        assertTrue(DslValidator.hasError(dslValidator.validate(PROD, withPrefix(PROD, onlyWorkflow.resultFiles(local, remote)))));

        // 워크플로우 + 쿼리 + 데이터베이스를 같이 고르면 통과하고, 고르지 않은 공통코드는 반영되지 않는다
        DslImportPlanner.Plan selected = plan.select(List.of(
                DslImportPlanner.keyOf("Workflow", "W001"),
                DslImportPlanner.keyOf("Query", "Q001"),
                DslImportPlanner.keyOf("Database", "MAIN_DB")));
        assertFalse(DslValidator.hasError(dslValidator.validate(PROD, withPrefix(PROD, selected.resultFiles(local, remote)))));

        DslImporter.Outcome outcome = dslImporter.apply(PROD, selected.changes());
        assertEquals(3, outcome.created());
        entityManager.clear();

        Map<String, String> prodFiles = relativeContents(PROD);
        assertEquals(remote.get("workflow/workflow/W001.json"), prodFiles.get("workflow/workflow/W001.json"));
        assertEquals("select 1", prodFiles.get("workflow/query/Q001/query.sql"));
        assertFalse(prodFiles.containsKey("setting/common-code/STATUS.json"));
    }

    @Test
    void 유형이_다른_화면은_미리보기에서_막는다() throws Exception {
        viewObjectRepository.save(ViewObject.builder().applicationId(DEV).objectCode("V9").objectName("화면").type(ObjectType.FILE).build());
        viewObjectRepository.save(ViewObject.builder().applicationId(PROD).objectCode("V9").objectName("화면").type(ObjectType.VIEW_BUILDER).build());
        entityManager.flush();
        entityManager.clear();

        DslImportPlanner.Plan plan = plan(PROD, DEV, relativeContents(DEV));
        List<DslIssue> issues = dslImporter.check(PROD, plan.changes());
        assertEquals(1, issues.size());
        assertTrue(issues.get(0).isError());
    }

    // ---- helpers ----

    private static void assertSameFiles(Map<String, String> expected, Map<String, String> actual) {
        StringBuilder diff = new StringBuilder();
        Set<String> paths = new TreeSet<>(expected.keySet());
        paths.addAll(actual.keySet());
        for(String path : paths) {
            if(!Objects.equals(expected.get(path), actual.get(path))) {
                diff.append("\n### ").append(path)
                        .append("\n--- expected\n").append(expected.get(path))
                        .append("\n--- actual\n").append(actual.get(path));
            }
        }
        assertEquals("", diff.toString());
    }

    private DslImportPlanner.Plan plan(String target, String source, Map<String, String> remote) {
        Map<String, DslFile> local = new TreeMap<>();
        for(DslFile file : dslExporter.export(target)) {
            local.put(file.path().substring(target.length() + 1), file);
        }
        return DslImportPlanner.plan(source, local, remote, true);
    }

    private Map<String, String> relativeContents(String applicationId) {
        Map<String, String> files = new TreeMap<>();
        for(DslFile file : dslExporter.export(applicationId)) {
            files.put(file.path().substring(applicationId.length() + 1), file.content());
        }
        return files;
    }

    private static Map<String, String> withPrefix(String applicationId, Map<String, String> files) {
        Map<String, String> result = new LinkedHashMap<>();
        files.forEach((path, content) -> result.put(applicationId + "/" + path, content));
        return result;
    }

    private static void remove(Map<String, String> files, String prefix) {
        files.keySet().removeIf(path -> path.startsWith(prefix));
    }

    private interface NodeEdit {
        void apply(ObjectNode node);
    }

    private static String edit(String json, NodeEdit edit) throws Exception {
        ObjectNode node = (ObjectNode) DslJson.read(json);
        edit.apply(node);
        return DslJson.write(node);
    }

    private void seedDev() {
        databaseRepository.save(DataSourceDatabaseMeta.builder()
                .applicationId(DEV).dataSourceName("MAIN_DB").displayName("메인").note("메모").databaseKind(DatabaseKind.MYSQL)
                .url("jdbc:mysql://dev-db").username("app").password("dev-secret").maximumPoolSize(10).minimumIdle(2)
                .connectionTimeout(3000).validationTimeout(1000).build());
        restServerRepository.save(DataSourceRestServer.builder()
                .applicationId(DEV).dataSourceName("API_SERVER").displayName("외부 API").baseUrl("https://api.example.com")
                .connectTimeout(1000).connectRequestTimeout(1000).connectionMaxTotal(10).connectionDefaultMaxPerRoute(5).build());
        notificationProviderRepository.save(NotificationProvider.builder()
                .applicationId(DEV).dataSourceName("MAIL").displayName("메일").type(NotificationType.SMTP)
                .options("{\"port\":\"587\",\"host\":\"smtp\",\"password\":\"dev-mail-secret\"}").build());

        CommonCodeKind codeKind = commonCodeKindRepository.save(CommonCodeKind.builder().applicationId(DEV).code("STATUS").name("상태").build());
        commonCodeRepository.save(CommonCode.builder().code("Y").name("사용").orderNum(1).codeKind(codeKind).build());
        commonCodeRepository.save(CommonCode.builder().code("N").name("미사용").orderNum(2).codeKind(codeKind).build());

        queryRepository.save(Query.builder()
                .applicationId(DEV).queryName("Q001").displayName("조회").dataSourceName("MAIN_DB").query("select 1\r\nfrom dual")
                .queryParams(new ArrayList<>(List.of(
                        QueryParam.builder().paramName("id").paramType(ParamType.STRING).autoValueType(AutoValueType.DEFAULT).inOut(InOut.IN).build(),
                        QueryParam.builder().paramName("name").paramType(ParamType.STRING).autoValueType(AutoValueType.DEFAULT).inOut(InOut.IN).build())))
                .build());
        entityRepository.save(Entity.builder()
                .applicationId(DEV).entityName("EMP").displayName("사원").dataSourceName("MAIN_DB").tableName("emp")
                .entityColumns(new ArrayList<>(List.of(
                        EntityColumn.builder().columnName("emp_id").displayName("사번").orderNum(1).useKey(true).columnType(ColumnType.STRING).build(),
                        EntityColumn.builder().columnName("emp_nm").displayName("이름").orderNum(2).useKey(false).columnType(ColumnType.STRING).build())))
                .build());
        restClientRepository.save(RestClient.builder()
                .applicationId(DEV).clientName("RC001").displayName("조회 API").dataSourceName("API_SERVER").method(HttpMethod.GET).path("/users")
                .queryParams(new ArrayList<>())
                .headers(new ArrayList<>(List.of(RestClientHeader.builder().name("X-Key").valueType(ValueType.INPUT_VALUE).inputValue("abc").build())))
                .body(new ArrayList<>())
                .build());
        notificationRepository.save(Notification.builder()
                .applicationId(DEV).notificationName("N001").displayName("알림").dataSourceName("MAIL").enableSend(true)
                .toAlias("to_address").subject("제목").content("내용").build());
        messageProcessorRepository.save(MessageProcessor.builder()
                .applicationId(DEV).processorName("P001").displayName("변환").processorType(ProcessorType.SCRIPT).script("return params;\n").build());

        Workflow sub = workflowRepository.save(Workflow.builder().applicationId(DEV).workflowCode("W_SUB").displayName("하위").useAuthValidation(false).build());
        workflowNodeRepository.save(WorkflowNode.builder().workflowId(sub.getId()).nodeId("s1").functionName("P001").functionType(FunctionType.MESSAGE_PROCESSOR)
                .orderNum(1).isLogging(false).requestMessageId("in").responseMessageId("out").build());

        Workflow workflow = workflowRepository.save(Workflow.builder().applicationId(DEV).workflowCode("W001").displayName("워크플로우").note("노트").useAuthValidation(true).build());
        Long workflowId = workflow.getId();
        workflowNodeRepository.save(WorkflowNode.builder().workflowId(workflowId).nodeId("n1").functionName("Q001").functionType(FunctionType.SQL)
                .orderNum(1).isLogging(true).requestMessageId("req").responseMessageId("res").positionX(10).positionY(20).build());
        workflowNodeRepository.save(WorkflowNode.builder().workflowId(workflowId).nodeId("c1").functionName("c1").functionType(FunctionType.CONDITION).orderNum(2).build());
        workflowNodeRepository.save(WorkflowNode.builder().workflowId(workflowId).nodeId("e1").functionName("e1").functionType(FunctionType.ERROR_MESSAGE).orderNum(3).build());
        workflowNodeRepository.save(WorkflowNode.builder().workflowId(workflowId).nodeId("w1").functionName("W_SUB").functionType(FunctionType.WORKFLOW).orderNum(4).build());
        workflowEdgeRepository.save(WorkflowEdge.builder().workflowId(workflowId).sourceNodeId("n1").targetNodeId("c1").branchType(BranchType.DEFAULT).orderNum(0).build());
        workflowEdgeRepository.save(WorkflowEdge.builder().workflowId(workflowId).sourceNodeId("c1").targetNodeId("e1").branchType(BranchType.CASE).branchId("b1").orderNum(1).build());
        workflowEdgeRepository.save(WorkflowEdge.builder().workflowId(workflowId).sourceNodeId("c1").targetNodeId("w1").branchType(BranchType.ELSE).orderNum(2).build());
        workflowConditionRepository.save(WorkflowCondition.builder().workflowId(workflowId).nodeId("c1").branchId("b1").conditionExpression("params.length == 0").orderNum(0).build());
        workflowErrorResponseRepository.save(WorkflowErrorResponse.builder().workflowId(workflowId).nodeId("e1").status(404).code("E404").message("없음").contents("req").build());
        workflowPermissionRepository.save(WorkflowPermission.builder().permissionCode("ADMIN").workflow(workflow).build());
        workflowDomainRepository.save(WorkflowDomain.builder().domain("*.example.com").workflow(workflow).build());

        RestApi restApi = restApiRepository.save(RestApi.builder()
                .applicationId(DEV).apiCode("API001").displayName("사원 조회").httpMethod(HttpMethodType.GET).path("/emp/{id}")
                .workflowCode("W001").enabled(true).responseType(ResponseType.JSON).build());
        restApiParameterRepository.save(RestApiParameter.builder().restApiId(restApi.getId()).paramIn(ParameterIn.PATH).paramKey("p1")
                .name("id").dataType(SchemaDataType.STRING).required(true).messageId("req").mappingKey("id").orderNum(0).build());

        SchedulerJob job = schedulerJobRepository.save(SchedulerJob.builder()
                .applicationId(DEV).jobCode("JOB_DAILY").jobName("매일").cronExpression("0 0 1 * * ?").enabled(false).build());
        schedulerJobWorkflowRepository.save(SchedulerJobWorkflow.builder().schedulerJob(job).workflow(workflow)
                .requestMessageJson("{\"req\":[{\"id\":\"1\"}]}").orderNum(1).build());

        ViewObject v1 = viewObjectRepository.save(ViewObject.builder().applicationId(DEV).objectCode("V001").objectName("화면")
                .type(ObjectType.VIEW_BUILDER).useAuthValidation(true).usePermissionValidation(true).useToolbar(true).build());
        viewObjectContentRepository.save(ViewObjectContent.builder().applicationId(DEV).objectCode("V001")
                .content("[{\"type\":\"row\",\"children\":[]}]").build());
        viewActionRepository.save(ViewAction.builder().applicationId(DEV).objectCode("V001").actionName("save").displayName("저장")
                .type("WORKFLOW").argsName("e").contents("{\"workflowCode\":\"W001\"}").script("alert(1);").build());
        viewCodeRepository.save(ViewCode.builder().applicationId(DEV).objectCode("V001").name("STATUS").target("statusSelect").type("SELECT").build());
        ViewObject v2 = viewObjectRepository.save(ViewObject.builder().applicationId(DEV).objectCode("V002").objectName("상세")
                .type(ObjectType.VIEW_BUILDER).useAuthValidation(false).usePermissionValidation(false).useToolbar(false).build());
        viewObjectRepository.save(ViewObject.builder().applicationId(DEV).objectCode("F001").objectName("파일 화면")
                .type(ObjectType.FILE).useAuthValidation(true).usePermissionValidation(false).useToolbar(true).build());
        viewSourceStorage.write(DEV, "F001", "<div>파일 화면</div>\r\n");

        Menu root = menuRepository.save(Menu.builder().applicationId(DEV).menuCd("ROOT").menuNm("상위").orderNum(1).icon("fa fa-folder").children(new ArrayList<>()).build());
        Menu m1 = menuRepository.save(Menu.builder().applicationId(DEV).menuCd("M001").menuNm("메뉴").orderNum(1).parentMenu(root).viewObject(v1).children(new ArrayList<>()).build());
        menuRepository.save(Menu.builder().applicationId(DEV).menuCd("M002").menuNm("하위 메뉴").orderNum(1).parentMenu(m1).viewObject(v2).children(new ArrayList<>()).build());
        menuPermissionRepository.save(MenuPermission.builder().permissionCode("ADMIN").menu(m1).build());

        layoutRepository.save(Layout.builder().applicationId(DEV).layoutTitle("EHR").homeObjectCode("V001").useLogo(true).logoText("EHR")
                .useCompanySelect(false).useProfile(true).profileImgSize(40).build());
    }
}
