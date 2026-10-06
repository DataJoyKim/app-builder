package com.prometis.appbuilder.app.view.dto;

import com.prometis.appbuilder.app.security.company.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class CompanyDto {
    @Getter @AllArgsConstructor @Builder
    public static class CompanyListResponse {
        // 현재 세션에 선택된 회사코드
        private String companyCode;
        private List<Company> companyList;
    }
}
