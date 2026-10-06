package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.app.security.appuser.AppUserRepository;
import com.prometis.appbuilder.app.security.company.Company;
import com.prometis.appbuilder.app.security.company.CompanyRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUser;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUserRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import com.prometis.appbuilder.security.token.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 애플리케이션 콘솔의 애플리케이션 사용자(AppUser) 등록/권한 변경/삭제 API.
 * 콘솔 인증 필터는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:app-user;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class AppUserRestControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    UserGroupUserRepository userGroupUserRepository;
    @Autowired
    JwtProvider jwtProvider;
    @Autowired
    CompanyRepository companyRepository;

    private User kim;
    private User lee;
    private User park;
    private User boss;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();
        companyRepository.deleteAll();
        userGroupUserRepository.deleteAll();
        userGroupRepository.deleteAll();

        kim = user("kim", "김사용");
        lee = user("lee", "이사용");
        park = user("park", "박사용");
        boss = user("boss", "요청자");
    }

    private User user(String loginId, String userName) {
        return userRepository.findByLoginId(loginId).orElseGet(() -> userRepository.save(User.builder()
                .loginId(loginId)
                .userName(userName)
                .password("x")
                .email(loginId + "@test.com")
                .build()));
    }

    private ResultActions register(String body) throws Exception {
        return mockMvc.perform(post("/ehr/console/api/app-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 여러_명을_한번에_애플리케이션_사용자로_등록한다() throws Exception {
        register("{\"userIds\":[" + kim.getId() + "," + lee.getId() + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(2))
                .andExpect(jsonPath("$.skipped").value(0));

        assertEquals(AppUser.AUTHORITY_APPLICATION_USER,
                appUserRepository.findByApplicationIdAndUserId("ehr", kim.getId()).orElseThrow().getAuthority());
        assertEquals(2, appUserRepository.findByApplicationId("ehr").size());
    }

    @Test
    void 이미_등록된_사용자는_건너뛰고_관리자_권한도_바꾸지_않는다() throws Exception {
        appUserRepository.save(AppUser.builder()
                .applicationId("ehr")
                .userId(kim.getId())
                .authority(AppUser.AUTHORITY_APPLICATION_ADMIN)
                .build());

        register("{\"userIds\":[" + kim.getId() + "," + lee.getId() + "," + lee.getId() + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(1))
                .andExpect(jsonPath("$.skipped").value(1));

        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN,
                appUserRepository.findByApplicationIdAndUserId("ehr", kim.getId()).orElseThrow().getAuthority());
        assertEquals(2, appUserRepository.findByApplicationId("ehr").size());
    }

    @Test
    void User에_없는_대상이_섞여_있으면_아무도_등록하지_않는다() throws Exception {
        register("{\"userIds\":[" + kim.getId() + ",-1]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자가 포함되어 있습니다."));

        assertEquals(0, appUserRepository.findAll().size());
    }

    @Test
    void 선택한_사용자가_없으면_400() throws Exception {
        register("{\"userIds\":[]}").andExpect(status().isBadRequest());
        register("{}").andExpect(status().isBadRequest());
    }

    @Test
    void 등록_후보는_이_애플리케이션에_아직_없는_사용자이고_비밀번호는_내보내지_않는다() throws Exception {
        register("{\"userIds\":[" + kim.getId() + "]}").andExpect(status().isOk());
        // 다른 애플리케이션에 등록된 것은 상관없다
        appUserRepository.save(AppUser.builder().applicationId("erp").userId(lee.getId()).authority(AppUser.AUTHORITY_APPLICATION_USER).build());

        mockMvc.perform(get("/ehr/console/api/app-user/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", not(hasItem(kim.getId().intValue()))))
                .andExpect(jsonPath("$[*].userId", hasItems(lee.getId().intValue(), park.getId().intValue())))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    private AppUser appUser(String applicationId, User user, String authority) {
        return appUserRepository.save(AppUser.builder().applicationId(applicationId).userId(user.getId()).authority(authority).build());
    }

    // 권한 변경/삭제는 로그인한 요청자(기본: boss)가 필요하다. 본인 여부를 가리기 때문
    private Cookie tokenOf(User user) {
        return new Cookie("accessToken", jwtProvider.generateAccessToken(user.getId()));
    }

    private ResultActions changeAuthority(String applicationId, Long id, String authority) throws Exception {
        return changeAuthority(applicationId, id, authority, boss);
    }

    private ResultActions changeAuthority(String applicationId, Long id, String authority, User requester) throws Exception {
        return mockMvc.perform(put("/" + applicationId + "/console/api/app-user/" + id + "/authority")
                .cookie(tokenOf(requester))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"authority\":\"" + authority + "\"}"));
    }

    private ResultActions deleteAppUser(String applicationId, Long id) throws Exception {
        return deleteAppUser(applicationId, id, boss);
    }

    private ResultActions deleteAppUser(String applicationId, Long id, User requester) throws Exception {
        return mockMvc.perform(delete("/" + applicationId + "/console/api/app-user/" + id)
                .cookie(tokenOf(requester)));
    }

    @Test
    void 본인의_권한은_변경하거나_삭제할_수_없다() throws Exception {
        AppUser self = appUser("ehr", boss, AppUser.AUTHORITY_APPLICATION_ADMIN);
        appUser("ehr", kim, AppUser.AUTHORITY_APPLICATION_ADMIN);

        changeAuthority("ehr", self.getId(), AppUser.AUTHORITY_APPLICATION_USER, boss)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("본인의 권한은 변경할 수 없습니다."));
        deleteAppUser("ehr", self.getId(), boss)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("본인은 삭제할 수 없습니다."));

        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN, appUserRepository.findById(self.getId()).orElseThrow().getAuthority());

        // 다른 관리자는 바꿀 수 있다
        changeAuthority("ehr", self.getId(), AppUser.AUTHORITY_APPLICATION_USER, kim)
                .andExpect(status().isOk());
    }

    @Test
    void 로그인하지_않으면_권한_변경과_삭제는_401() throws Exception {
        AppUser target = appUser("ehr", lee, AppUser.AUTHORITY_APPLICATION_USER);

        mockMvc.perform(put("/ehr/console/api/app-user/" + target.getId() + "/authority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authority\":\"APPLICATION_ADMIN\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/ehr/console/api/app-user/" + target.getId()))
                .andExpect(status().isUnauthorized());

        assertTrue(appUserRepository.findById(target.getId()).isPresent());
    }

    @Test
    void 목록에서_본인_행을_표시한다() throws Exception {
        appUser("ehr", boss, AppUser.AUTHORITY_APPLICATION_ADMIN);
        appUser("ehr", kim, AppUser.AUTHORITY_APPLICATION_USER);

        mockMvc.perform(get("/ehr/console/api/app-user").cookie(tokenOf(boss)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == " + boss.getId() + ")].me", contains(true)))
                .andExpect(jsonPath("$[?(@.userId == " + kim.getId() + ")].me", contains(false)));
    }

    @Test
    void 사용자를_관리자로_올리고_다시_사용자로_낮춘다() throws Exception {
        appUser("ehr", kim, AppUser.AUTHORITY_APPLICATION_ADMIN);
        AppUser target = appUser("ehr", lee, AppUser.AUTHORITY_APPLICATION_USER);

        changeAuthority("ehr", target.getId(), AppUser.AUTHORITY_APPLICATION_ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority").value(AppUser.AUTHORITY_APPLICATION_ADMIN))
                .andExpect(jsonPath("$.userName").value("이사용"));
        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN, appUserRepository.findById(target.getId()).orElseThrow().getAuthority());

        changeAuthority("ehr", target.getId(), AppUser.AUTHORITY_APPLICATION_USER)
                .andExpect(status().isOk());
        assertEquals(AppUser.AUTHORITY_APPLICATION_USER, appUserRepository.findById(target.getId()).orElseThrow().getAuthority());
    }

    @Test
    void 허용되지_않은_권한으로는_바꾸지_않는다() throws Exception {
        AppUser target = appUser("ehr", lee, AppUser.AUTHORITY_APPLICATION_USER);

        changeAuthority("ehr", target.getId(), "PLATFORM_ADMIN")
                .andExpect(status().isBadRequest());
        assertEquals(AppUser.AUTHORITY_APPLICATION_USER, appUserRepository.findById(target.getId()).orElseThrow().getAuthority());
    }

    @Test
    void 마지막_관리자는_사용자로_낮추거나_삭제할_수_없다() throws Exception {
        AppUser admin = appUser("ehr", kim, AppUser.AUTHORITY_APPLICATION_ADMIN);
        // 다른 애플리케이션의 관리자는 세지 않는다
        appUser("erp", lee, AppUser.AUTHORITY_APPLICATION_ADMIN);

        changeAuthority("ehr", admin.getId(), AppUser.AUTHORITY_APPLICATION_USER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("마지막 관리자")));
        deleteAppUser("ehr", admin.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("마지막 관리자")));

        assertEquals(AppUser.AUTHORITY_APPLICATION_ADMIN, appUserRepository.findById(admin.getId()).orElseThrow().getAuthority());

        // 관리자가 둘이면 한 명은 삭제할 수 있다
        appUser("ehr", park, AppUser.AUTHORITY_APPLICATION_ADMIN);
        deleteAppUser("ehr", admin.getId())
                .andExpect(status().isOk());
        assertTrue(appUserRepository.findById(admin.getId()).isEmpty());
    }

    @Test
    void 사용자를_삭제하면_다시_등록_후보가_된다() throws Exception {
        AppUser target = appUser("ehr", lee, AppUser.AUTHORITY_APPLICATION_USER);

        deleteAppUser("ehr", target.getId())
                .andExpect(status().isOk());

        assertTrue(appUserRepository.findById(target.getId()).isEmpty());
        mockMvc.perform(get("/ehr/console/api/app-user/candidates"))
                .andExpect(jsonPath("$[*].userId", hasItem(lee.getId().intValue())));
    }

    @Test
    void 삭제하면_그_애플리케이션의_사용자_그룹_소속도_지운다() throws Exception {
        UserGroup ehrGroup = userGroupRepository.save(UserGroup.builder().applicationId("ehr").companyCode("C001").code("EHR-DEL-G").name("인사팀").build());
        UserGroup erpGroup = userGroupRepository.save(UserGroup.builder().applicationId("erp").companyCode("C001").code("ERP-DEL-G").name("재무팀").build());
        UserGroupUser ehrMembership = userGroupUserRepository.save(UserGroupUser.builder().userGroup(ehrGroup).user(lee).build());
        UserGroupUser erpMembership = userGroupUserRepository.save(UserGroupUser.builder().userGroup(erpGroup).user(lee).build());
        // 같은 그룹의 다른 사람 소속은 남는다
        UserGroupUser otherMembership = userGroupUserRepository.save(UserGroupUser.builder().userGroup(ehrGroup).user(park).build());

        AppUser target = appUser("ehr", lee, AppUser.AUTHORITY_APPLICATION_USER);

        deleteAppUser("ehr", target.getId())
                .andExpect(status().isOk());

        assertTrue(userGroupUserRepository.findById(ehrMembership.getId()).isEmpty());
        assertTrue(userGroupUserRepository.findById(erpMembership.getId()).isPresent());
        assertTrue(userGroupUserRepository.findById(otherMembership.getId()).isPresent());
    }

    @Test
    void 마지막_관리자를_삭제하지_못하면_그룹_소속도_남는다() throws Exception {
        UserGroup ehrGroup = userGroupRepository.save(UserGroup.builder().applicationId("ehr").companyCode("C001").code("EHR-KEEP-G").name("관리팀").build());
        UserGroupUser membership = userGroupUserRepository.save(UserGroupUser.builder().userGroup(ehrGroup).user(kim).build());
        AppUser admin = appUser("ehr", kim, AppUser.AUTHORITY_APPLICATION_ADMIN);

        deleteAppUser("ehr", admin.getId())
                .andExpect(status().isBadRequest());

        assertTrue(userGroupUserRepository.findById(membership.getId()).isPresent());
    }

    @Test
    void 다른_애플리케이션의_사용자는_바꾸거나_삭제하지_않는다() throws Exception {
        AppUser other = appUser("erp", lee, AppUser.AUTHORITY_APPLICATION_USER);

        changeAuthority("ehr", other.getId(), AppUser.AUTHORITY_APPLICATION_ADMIN)
                .andExpect(status().isBadRequest());
        deleteAppUser("ehr", other.getId())
                .andExpect(status().isBadRequest());

        AppUser saved = appUserRepository.findById(other.getId()).orElseThrow();
        assertEquals(AppUser.AUTHORITY_APPLICATION_USER, saved.getAuthority());
    }

    @Test
    void 등록된_사용자_목록은_그_애플리케이션_것만_사용자정보와_함께_준다() throws Exception {
        register("{\"userIds\":[" + kim.getId() + "]}").andExpect(status().isOk());
        appUserRepository.save(AppUser.builder().applicationId("erp").userId(lee.getId()).authority(AppUser.AUTHORITY_APPLICATION_USER).build());

        mockMvc.perform(get("/ehr/console/api/app-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(kim.getId()))
                .andExpect(jsonPath("$[0].userName").value("김사용"))
                .andExpect(jsonPath("$[0].authority").value(AppUser.AUTHORITY_APPLICATION_USER));
    }

    // ---- 소속 회사 ----

    private ResultActions changeCompany(String applicationId, Long id, String companyCode, User requester) throws Exception {
        String body = companyCode == null ? "{}" : "{\"companyCode\":\"" + companyCode + "\"}";
        return mockMvc.perform(put("/" + applicationId + "/console/api/app-user/" + id + "/company")
                .cookie(tokenOf(requester))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 회사_미지정_사용자의_회사를_지정하고_다시_비운다() throws Exception {
        companyRepository.save(Company.builder().applicationId("ehr").companyCode("C001").companyName("본사").build());
        AppUser target = appUser("ehr", kim, AppUser.AUTHORITY_APPLICATION_USER);

        mockMvc.perform(get("/ehr/console/api/app-user").cookie(tokenOf(boss)))
                .andExpect(jsonPath("$[0].companyCode").doesNotExist());

        changeCompany("ehr", target.getId(), "C001", boss)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyCode").value("C001"));
        assertEquals("C001", appUserRepository.findById(target.getId()).orElseThrow().getCompanyCode());

        // 비우면 회사 미지정
        changeCompany("ehr", target.getId(), "", boss)
                .andExpect(status().isOk());
        assertNull(appUserRepository.findById(target.getId()).orElseThrow().getCompanyCode());
    }

    @Test
    void 등록되지_않은_회사나_다른_애플리케이션의_회사로는_바꾸지_않는다() throws Exception {
        companyRepository.save(Company.builder().applicationId("crm").companyCode("C001").companyName("CRM 회사").build());
        AppUser target = appUser("ehr", kim, AppUser.AUTHORITY_APPLICATION_USER);

        changeCompany("ehr", target.getId(), "C001", boss)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("등록되지 않은 회사")));

        assertNull(appUserRepository.findById(target.getId()).orElseThrow().getCompanyCode());
    }

    @Test
    void 다른_애플리케이션의_사용자_회사는_바꾸지_않는다() throws Exception {
        companyRepository.save(Company.builder().applicationId("ehr").companyCode("C001").companyName("본사").build());
        AppUser other = appUser("crm", kim, AppUser.AUTHORITY_APPLICATION_USER);

        changeCompany("ehr", other.getId(), "C001", boss)
                .andExpect(status().isBadRequest());
    }
}
