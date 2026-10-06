package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.company.Company;
import com.prometis.appbuilder.app.security.company.CompanyRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사용자 그룹은 회사별로 관리한다: (applicationId, companyCode, code) 가 유일하고 상위 그룹도 같은 회사 안에서만 고른다.
 * 콘솔 인증 필터는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-group;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class UserGroupRestControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        userGroupRepository.findAll().stream().filter(group -> group.getParentUserGroup() != null).forEach(userGroupRepository::delete);
        userGroupRepository.deleteAll();
        companyRepository.deleteAll();

        companyRepository.save(Company.builder().applicationId("ehr").companyCode("C001").companyName("본사").build());
        companyRepository.save(Company.builder().applicationId("ehr").companyCode("C002").companyName("자회사").build());
    }

    private ResultActions create(String companyCode, String code, String parentUserGroupCode) throws Exception {
        String body = "{\"companyCode\":\"" + companyCode + "\",\"code\":\"" + code + "\",\"name\":\"" + code + "\""
                + (parentUserGroupCode == null ? "" : ",\"parentUserGroupCode\":\"" + parentUserGroupCode + "\"") + "}";
        return mockMvc.perform(post("/ehr/console/api/user-group")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 같은_코드라도_회사가_다르면_따로_만든다() throws Exception {
        create("C001", "SALES", null).andExpect(status().isOk());
        create("C002", "SALES", null).andExpect(status().isOk());

        create("C001", "SALES", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("이미")));

        assertEquals(2, userGroupRepository.findByApplicationId("ehr").size());
    }

    @Test
    void 등록되지_않은_회사에는_만들지_않는다() throws Exception {
        create("C999", "SALES", null).andExpect(status().isBadRequest());

        assertTrue(userGroupRepository.findByApplicationId("ehr").isEmpty());
    }

    @Test
    void 트리는_회사별로_조회한다() throws Exception {
        create("C001", "SALES", null).andExpect(status().isOk());
        create("C001", "SALES_1", "SALES").andExpect(status().isOk());
        create("C002", "HR", null).andExpect(status().isOk());

        mockMvc.perform(get("/ehr/console/api/user-group/tree").param("companyCode", "C001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("SALES"))
                .andExpect(jsonPath("$[0].companyCode").value("C001"))
                .andExpect(jsonPath("$[0].children[0].code").value("SALES_1"));

        // companyCode 가 없으면 모든 회사
        mockMvc.perform(get("/ehr/console/api/user-group/tree"))
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/ehr/console/api/user-group").param("companyCode", "C002"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("HR"));
    }

    @Test
    void 상위_그룹은_같은_회사에서만_찾는다() throws Exception {
        create("C002", "SALES", null).andExpect(status().isOk());
        create("C001", "SALES_1", "SALES").andExpect(status().isOk());

        UserGroup child = userGroupRepository.findByApplicationIdAndCompanyCodeAndCode("ehr", "C001", "SALES_1").orElseThrow();
        assertNull(child.getParentUserGroup());
    }

    @Test
    void 수정할_때_회사는_바뀌지_않고_같은_회사_안에서_코드_중복을_막는다() throws Exception {
        create("C001", "SALES", null).andExpect(status().isOk());
        create("C001", "HR", null).andExpect(status().isOk());
        UserGroup hr = userGroupRepository.findByApplicationIdAndCompanyCodeAndCode("ehr", "C001", "HR").orElseThrow();

        mockMvc.perform(put("/ehr/console/api/user-group/" + hr.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyCode\":\"C002\",\"code\":\"SALES\",\"name\":\"인사\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/ehr/console/api/user-group/" + hr.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyCode\":\"C002\",\"code\":\"HR2\",\"name\":\"인사\"}"))
                .andExpect(status().isOk());

        UserGroup saved = userGroupRepository.findById(hr.getId()).orElseThrow();
        assertEquals("C001", saved.getCompanyCode());
        assertEquals("HR2", saved.getCode());
    }
}
