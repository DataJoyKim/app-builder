package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.permission.Permission;
import com.prometis.appbuilder.app.security.permission.PermissionRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupPermission;
import com.prometis.appbuilder.app.security.usergroup.UserGroupPermissionRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 권한 화면에서 사용자 그룹을 연결(UserGroupPermission)하는 API.
 * 콘솔 인증 필터는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-group-permission;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class UserGroupPermissionRestControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    PermissionRepository permissionRepository;
    @Autowired
    UserGroupPermissionRepository userGroupPermissionRepository;

    private Permission permission;
    private UserGroup sales;
    private UserGroup salesTeam;

    @BeforeEach
    void setUp() {
        userGroupPermissionRepository.deleteAll();
        userGroupRepository.findAll().stream().filter(group -> group.getParentUserGroup() != null).forEach(userGroupRepository::delete);
        userGroupRepository.deleteAll();
        permissionRepository.deleteAll();

        permission = permissionRepository.save(Permission.builder().applicationId("ehr").code("ORDER_READ").name("주문 조회").build());
        sales = userGroupRepository.save(UserGroup.builder().applicationId("ehr").companyCode("C001").code("SALES").name("영업본부").build());
        salesTeam = userGroupRepository.save(UserGroup.builder().applicationId("ehr").companyCode("C001").code("SALES_1").name("영업1팀").parentUserGroup(sales).build());
    }

    private ResultActions bind(String applicationId, Object userGroupId, Object permissionId, boolean lowerPermissionGrant) throws Exception {
        String body = "{\"userGroupId\":" + json(userGroupId) + ",\"permissionId\":" + json(permissionId)
                + ",\"lowerPermissionGrant\":" + lowerPermissionGrant + "}";
        return mockMvc.perform(post("/" + applicationId + "/console/api/user-group/permission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String json(Object value) {
        return value instanceof String ? "\"" + value + "\"" : String.valueOf(value);
    }

    @Test
    void 권한에_사용자_그룹을_연결하고_권한별로_조회한다() throws Exception {
        // 화면에 따라 id 를 문자열 또는 숫자로 보낸다
        bind("ehr", String.valueOf(sales.getId()), String.valueOf(permission.getId()), true).andExpect(status().isOk());
        bind("ehr", salesTeam.getId(), permission.getId(), false).andExpect(status().isOk());

        mockMvc.perform(get("/ehr/console/api/user-group/permission/by-permission/" + permission.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.userGroup.code == 'SALES')].lowerPermissionGrant", contains(true)))
                .andExpect(jsonPath("$[?(@.userGroup.code == 'SALES_1')].lowerPermissionGrant", contains(false)));
    }

    @Test
    void 같은_그룹에_같은_권한을_두_번_연결하지_않는다() throws Exception {
        bind("ehr", sales.getId(), permission.getId(), false).andExpect(status().isOk());

        bind("ehr", sales.getId(), permission.getId(), true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("이미")));

        assertEquals(1, userGroupPermissionRepository.findByPermissionId(permission.getId()).size());
    }

    @Test
    void 하위_그룹_전파_여부를_바꾼다() throws Exception {
        bind("ehr", sales.getId(), permission.getId(), false).andExpect(status().isOk());
        UserGroupPermission saved = userGroupPermissionRepository.findByPermissionId(permission.getId()).get(0);

        mockMvc.perform(put("/ehr/console/api/user-group/permission/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userGroupId\":" + sales.getId() + ",\"permissionId\":" + permission.getId() + ",\"lowerPermissionGrant\":true}"))
                .andExpect(status().isOk());

        assertTrue(userGroupPermissionRepository.findById(saved.getId()).orElseThrow().getLowerPermissionGrant());
    }

    @Test
    void 다른_애플리케이션의_연결은_보이지_않는다() throws Exception {
        bind("ehr", sales.getId(), permission.getId(), false).andExpect(status().isOk());

        mockMvc.perform(get("/crm/console/api/user-group/permission/by-permission/" + permission.getId()))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void 연결을_해제한다() throws Exception {
        bind("ehr", sales.getId(), permission.getId(), false).andExpect(status().isOk());
        List<UserGroupPermission> saved = userGroupPermissionRepository.findByPermissionId(permission.getId());

        mockMvc.perform(delete("/ehr/console/api/user-group/permission/" + saved.get(0).getId()))
                .andExpect(status().isOk());

        assertTrue(userGroupPermissionRepository.findByPermissionId(permission.getId()).isEmpty());
    }
}
