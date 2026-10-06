package com.prometis.appbuilder.app.security.company;

import com.prometis.appbuilder.app.security.usergroup.UserGroup;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUser;
import com.prometis.appbuilder.app.security.usergroup.UserGroupUserRepository;
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
    private final UserGroupUserRepository userGroupUserRepository;

    /**
     * 사용자가 속한 사용자 그룹들의 회사 (중복 제거, 순서(orderNum) 순).
     * 그룹에 적힌 회사코드라도 Company 로 등록되지 않은 회사는 빠진다.
     */
    public List<Company> getUserCompanies(String applicationId, Long userId) {
        Set<String> companyCodes = userGroupUserRepository.findByUserIdAndUserGroupApplicationId(userId, applicationId).stream()
                .map(UserGroupUser::getUserGroup)
                .filter(Objects::nonNull)
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
