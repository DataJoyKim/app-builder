package com.prometis.appbuilder.console.app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 이메일 초대. authority 는 모두에게 같은 권한(APPLICATION_ADMIN / APPLICATION_USER), companyCode 는 모두가 소속될 회사(비어 있으면 미지정)다.
 */
@Getter
@NoArgsConstructor
public class AppInvitationBatchRequest {
    private List<String> emails;
    private String authority;
    private String companyCode;
}
