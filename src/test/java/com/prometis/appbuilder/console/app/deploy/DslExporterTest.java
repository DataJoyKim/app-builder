package com.prometis.appbuilder.console.app.deploy;

import com.fasterxml.jackson.databind.JsonNode;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMeta;
import com.prometis.appbuilder.app.datasource.database.DataSourceDatabaseMetaRepository;
import com.prometis.appbuilder.app.datasource.database.DatabaseKind;
import com.prometis.appbuilder.app.datasource.notification.NotificationProvider;
import com.prometis.appbuilder.app.datasource.notification.NotificationProviderRepository;
import com.prometis.appbuilder.app.executor.notification.NotificationType;
import com.prometis.appbuilder.app.image.ImageStorage;
import com.prometis.appbuilder.app.image.ImageStorageRepository;
import com.prometis.appbuilder.app.image.StorageType;
import com.prometis.appbuilder.app.image.StorageTypeRepository;
import com.prometis.appbuilder.app.node.WorkflowNode;
import com.prometis.appbuilder.app.node.WorkflowNodeRepository;
import com.prometis.appbuilder.app.node.code.FunctionType;
import com.prometis.appbuilder.app.query.Query;
import com.prometis.appbuilder.app.query.QueryParam;
import com.prometis.appbuilder.app.query.QueryRepository;
import com.prometis.appbuilder.app.query.code.AutoValueType;
import com.prometis.appbuilder.app.query.code.InOut;
import com.prometis.appbuilder.app.query.code.ParamType;
import com.prometis.appbuilder.app.view.MenuRepository;
import com.prometis.appbuilder.app.view.ViewActionRepository;
import com.prometis.appbuilder.app.view.ViewObjectRepository;
import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.domain.ViewAction;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowPermission;
import com.prometis.appbuilder.app.workflow.WorkflowPermissionRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import com.prometis.appbuilder.console.app.deploy.dsl.DslExporter;
import com.prometis.appbuilder.console.app.deploy.dsl.DslFile;
import com.prometis.appbuilder.console.app.deploy.dsl.DslJson;
import com.prometis.appbuilder.console.app.deploy.dsl.DslValidator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 실제 메타데이터 테이블(H2)에서 DSL 파일을 만들고, 파일 경로/내용/민감정보 제외/검증 통과를 확인한다.
 */
@DataJpaTest
@Import({DslExporter.class, DslValidator.class})
class DslExporterTest {
    private static final String APP = "ehr";

    @Autowired
    private DslExporter dslExporter;
    @Autowired
    private DslValidator dslValidator;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSourceDatabaseMetaRepository databaseRepository;
    @Autowired
    private NotificationProviderRepository notificationProviderRepository;
    @Autowired
    private QueryRepository queryRepository;
    @Autowired
    private WorkflowRepository workflowRepository;
    @Autowired
    private WorkflowNodeRepository workflowNodeRepository;
    @Autowired
    private WorkflowPermissionRepository workflowPermissionRepository;
    @Autowired
    private ViewObjectRepository viewObjectRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private ViewActionRepository viewActionRepository;
    @Autowired
    private StorageTypeRepository storageTypeRepository;
    @Autowired
    private ImageStorageRepository imageStorageRepository;

    @Test
    void 메타데이터를_ID_파일명의_DSL로_만든다() throws Exception {
        databaseRepository.save(DataSourceDatabaseMeta.builder()
                .applicationId(APP).dataSourceName("MAIN_DB").displayName("메인").databaseKind(DatabaseKind.MYSQL)
                .url("jdbc:mysql://db").username("app").password("db-secret").build());
        notificationProviderRepository.save(NotificationProvider.builder()
                .applicationId(APP).dataSourceName("MAIL").displayName("메일").type(NotificationType.SMTP)
                .options("{\"port\":\"587\",\"host\":\"smtp\",\"password\":\"mail-secret\"}").build());
        queryRepository.save(Query.builder()
                .applicationId(APP).queryName("Q001").displayName("조회").dataSourceName("MAIN_DB").query("select 1")
                .queryParams(List.of(QueryParam.builder().paramName("id").paramType(ParamType.STRING).autoValueType(AutoValueType.DEFAULT).inOut(InOut.IN).build()))
                .build());

        Workflow workflow = workflowRepository.save(Workflow.builder()
                .applicationId(APP).workflowCode("W001").displayName("워크플로우").useAuthValidation(true).build());
        workflowNodeRepository.save(WorkflowNode.builder()
                .workflowId(workflow.getId()).nodeId("n1").functionName("Q001").functionType(FunctionType.SQL).orderNum(1).build());
        workflowPermissionRepository.save(WorkflowPermission.builder().permissionCode("ADMIN").workflow(workflow).build());

        ViewObject viewObject = viewObjectRepository.save(ViewObject.builder()
                .applicationId(APP).objectCode("V001").objectName("화면").build());
        viewActionRepository.save(ViewAction.builder()
                .applicationId(APP).objectCode("V001").actionName("save").displayName("저장").type("BUTTON").script("alert(1);").build());
        viewActionRepository.save(ViewAction.builder()
                .applicationId(APP).objectCode("V001").actionName("noScript").displayName("스크립트 없음").type("BUTTON").build());
        Menu parent = menuRepository.save(Menu.builder().applicationId(APP).menuCd("ROOT").menuNm("상위").build());
        menuRepository.save(Menu.builder().applicationId(APP).menuCd("M001").menuNm("메뉴").parentMenu(parent).viewObject(viewObject).build());

        storageTypeRepository.save(StorageType.builder().applicationId(APP).storageType("LOGO").displayName("로고").build());
        imageStorageRepository.save(ImageStorage.builder()
                .applicationId(APP).storageType("LOGO").filename("main").originalFilename("main.png").fileExtension("png")
                .fileSize(3L).content(new byte[]{1, 2, 3}).build());

        // 다른 애플리케이션 것은 나오지 않는다
        workflowRepository.save(Workflow.builder()
                .applicationId("other").workflowCode("W999").displayName("다른 앱").useAuthValidation(false).build());

        entityManager.flush();
        entityManager.clear();

        List<DslFile> files = dslExporter.export(APP);
        Map<String, String> byPath = files.stream().collect(Collectors.toMap(DslFile::path, DslFile::content));

        assertEquals(List.of(
                "ehr/datasource/database/MAIN_DB.json",
                "ehr/datasource/notification-provider/MAIL.json",
                "ehr/setting/image-manage/LOGO.json",
                "ehr/view/menu/M001.json",
                "ehr/view/menu/ROOT.json",
                "ehr/view/view-object/V001/actions/noScript/metadata.json",
                "ehr/view/view-object/V001/actions/save/metadata.json",
                "ehr/view/view-object/V001/actions/save/script.js",
                "ehr/view/view-object/V001/metadata.json",
                "ehr/workflow/query/Q001/metadata.json",
                "ehr/workflow/query/Q001/query.sql",
                "ehr/workflow/workflow/W001.json"
        ), files.stream().map(DslFile::path).toList());

        String database = byPath.get("ehr/datasource/database/MAIN_DB.json");
        assertFalse(database.contains("db-secret"));
        assertFalse(database.contains("password"));

        JsonNode mail = DslJson.read(byPath.get("ehr/datasource/notification-provider/MAIL.json"));
        assertEquals("smtp", mail.path("options").path("host").asText());
        assertFalse(mail.path("options").has("password"));

        JsonNode workflowDsl = DslJson.read(byPath.get("ehr/workflow/workflow/W001.json"));
        assertEquals("Workflow", workflowDsl.path("kind").asText());
        assertEquals("ADMIN", workflowDsl.path("permissions").get(0).asText());
        assertEquals("Q001", workflowDsl.path("nodes").get(0).path("functionName").asText());
        assertFalse(workflowDsl.has("id"));
        assertFalse(workflowDsl.has("applicationId"));

        JsonNode query = DslJson.read(byPath.get("ehr/workflow/query/Q001/metadata.json"));
        assertFalse(query.has("query"));
        assertEquals("select 1", byPath.get("ehr/workflow/query/Q001/query.sql"));
        assertEquals("id", query.path("params").get(0).path("paramName").asText());

        JsonNode view = DslJson.read(byPath.get("ehr/view/view-object/V001/metadata.json"));
        assertFalse(view.has("actions"));
        JsonNode saveAction = DslJson.read(byPath.get("ehr/view/view-object/V001/actions/save/metadata.json"));
        assertEquals("ViewAction", saveAction.path("kind").asText());
        assertEquals("save", saveAction.path("actionName").asText());
        assertFalse(saveAction.has("script"));
        assertEquals("alert(1);", byPath.get("ehr/view/view-object/V001/actions/save/script.js"));

        JsonNode menu = DslJson.read(byPath.get("ehr/view/menu/M001.json"));
        assertEquals("ROOT", menu.path("parentMenuCd").asText());
        assertEquals("V001", menu.path("objectCode").asText());

        JsonNode logo = DslJson.read(byPath.get("ehr/setting/image-manage/LOGO.json"));
        assertEquals("AQID", logo.path("images").get(0).path("content").asText());

        assertEquals(List.of(), dslValidator.validate(APP, byPath));

        // 같은 메타데이터면 같은 파일이 나온다 (비교가 흔들리지 않는다)
        entityManager.clear();
        assertEquals(files, dslExporter.export(APP));
    }
}
