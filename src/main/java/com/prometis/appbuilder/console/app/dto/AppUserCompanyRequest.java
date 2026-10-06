package com.prometis.appbuilder.console.app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 애플리케이션 사용자의 소속 회사 변경. companyCode 가 비어 있으면 회사 미지정으로 바꾼다.
 */
@Getter
@NoArgsConstructor
public class AppUserCompanyRequest {
    private String companyCode;
}
