package com.prometis.appbuilder.console.app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 이메일 초대. authority 는 모두에게 같은 권한(APPLICATION_ADMIN / APPLICATION_USER)이다.
 */
@Getter
@NoArgsConstructor
public class AppInvitationBatchRequest {
    private List<String> emails;
    private String authority;
}
