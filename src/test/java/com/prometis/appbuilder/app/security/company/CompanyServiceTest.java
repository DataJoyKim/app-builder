package com.prometis.appbuilder.app.security.company;

import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupRepository;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUser;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUserRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(CompanyService.class)
class CompanyServiceTest {
    @Autowired
    CompanyService companyService;
    @Autowired
    CompanyRepository companyRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    UserGroupUserRepository userGroupUserRepository;

    private UserGroup group(String applicationId, String companyCode, String code) {
        return userGroupRepository.save(UserGroup.builder().applicationId(applicationId).companyCode(companyCode).code(code).name(code).build());
    }

    private void join(User user, UserGroup group) {
        userGroupUserRepository.save(UserGroupUser.builder().user(user).userGroup(group).build());
    }

    private void company(String applicationId, String companyCode, String companyName) {
        companyRepository.save(Company.builder().applicationId(applicationId).companyCode(companyCode).companyName(companyName).build());
    }

    @Test
    void 사용자가_속한_그룹의_회사만_중복없이_조회한다() {
        User user = userRepository.save(User.builder().loginId("company-user").userName("홍길동").password("pw").email("company-user@test.com").build());

        company("ehr", "C001", "본사");
        company("ehr", "C002", "자회사");
        company("ehr", "C003", "속하지 않은 회사");
        company("crm", "C009", "다른 애플리케이션 회사");

        join(user, group("ehr", "C002", "HR"));
        join(user, group("ehr", "C001", "SALES"));
        join(user, group("ehr", "C001", "SALES_1"));   // 같은 회사 그룹 두 개 → 한 번만
        join(user, group("ehr", "C404", "GHOST"));     // 등록되지 않은 회사 → 빠짐
        join(user, group("crm", "C009", "CRM"));       // 다른 애플리케이션 → 빠짐

        List<Company> companies = companyService.getUserCompanies("ehr", user.getId());

        assertEquals(List.of("C001", "C002"), companies.stream().map(Company::getCompanyCode).toList());
    }

    @Test
    void 회사는_순서대로_나오고_순서가_없는_회사는_맨_뒤에_회사코드_순으로_나온다() {
        User user = userRepository.save(User.builder().loginId("company-order").userName("순서").password("pw").email("company-order@test.com").build());

        companyRepository.save(Company.builder().applicationId("ehr").companyCode("A001").companyName("순서없음A").build());
        companyRepository.save(Company.builder().applicationId("ehr").companyCode("B001").companyName("셋째").orderNum(3).build());
        companyRepository.save(Company.builder().applicationId("ehr").companyCode("C001").companyName("첫째").orderNum(0).build());
        companyRepository.save(Company.builder().applicationId("ehr").companyCode("D001").companyName("둘째").orderNum(1).build());
        companyRepository.save(Company.builder().applicationId("ehr").companyCode("E001").companyName("순서없음E").build());

        for(String companyCode : List.of("A001", "B001", "C001", "D001", "E001")) {
            join(user, group("ehr", companyCode, "G-" + companyCode));
        }

        List<String> expected = List.of("C001", "D001", "B001", "A001", "E001");
        assertEquals(expected, companyService.getUserCompanies("ehr", user.getId()).stream().map(Company::getCompanyCode).toList());
        assertEquals(expected, companyRepository.findOrderedByApplicationId("ehr").stream().map(Company::getCompanyCode).toList());
    }

    @Test
    void 그룹에_속하지_않으면_빈_목록() {
        User user = userRepository.save(User.builder().loginId("no-group").userName("무소속").password("pw").email("no-group@test.com").build());
        company("ehr", "C001", "본사");

        assertTrue(companyService.getUserCompanies("ehr", user.getId()).isEmpty());
    }
}
