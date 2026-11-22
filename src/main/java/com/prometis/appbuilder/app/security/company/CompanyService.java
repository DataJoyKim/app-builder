package com.prometis.appbuilder.app.security.company;

import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserGroupService userGroupService;

    /**
     * 사용자가 속한 사용자 그룹들의 회사 (중복 제거, 순서(orderNum) 순).
     * 그룹에 적힌 회사코드라도 Company 로 등록되지 않은 회사는 빠진다.
     */
    public List<Company> getUserCompanies(String applicationId, Long userId) {
        // "모든 사용자"가 매핑된 그룹도 이 사용자의 그룹이므로 그 회사도 고를 수 있다 (UserGroupService.getAppliedUserGroups)
        Set<String> companyCodes = userGroupService.getAppliedUserGroups(applicationId, userId).stream()
                .map(UserGroup::getCompanyCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if(companyCodes.isEmpty()) {
            return List.of();
        }

        return companyRepository.findOrderedByApplicationIdAndCompanyCodeIn(applicationId, companyCodes);
    }

    /**
     * 사용자가 해당 회사를 선택할 수 있는지 (getUserCompanies 에 들어가는 회사인지).
     */
    public boolean hasUserCompany(String applicationId, Long userId, String companyCode) {
        if(companyCode == null || companyCode.isEmpty()) {
            return false;
        }

        return getUserCompanies(applicationId, userId).stream()
                .anyMatch(company -> companyCode.equals(company.getCompanyCode()));
    }
}
