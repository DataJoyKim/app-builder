package com.prometis.appbuilder.app.security.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    // 순서(orderNum) 순, 순서가 없는 회사는 뒤로, 같은 순서면 회사코드 순
    String ORDER = " ORDER BY CASE WHEN c.orderNum IS NULL THEN 1 ELSE 0 END, c.orderNum ASC, c.companyCode ASC";

    @Query("select c from Company c where c.applicationId = :applicationId" + ORDER)
    List<Company> findOrderedByApplicationId(@Param("applicationId") String applicationId);

    @Query("select c from Company c where c.applicationId = :applicationId and c.companyCode in :companyCodes" + ORDER)
    List<Company> findOrderedByApplicationIdAndCompanyCodeIn(@Param("applicationId") String applicationId, @Param("companyCodes") Collection<String> companyCodes);

    Optional<Company> findByApplicationIdAndCompanyCode(String applicationId, String companyCode);
}
