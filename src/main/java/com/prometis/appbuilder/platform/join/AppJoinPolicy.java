package com.prometis.appbuilder.platform.join;

/**
 * 애플리케이션 가입 방식 (애플리케이션별 설정, 앱 가입 페이지 /{applicationId}/signup 에 적용).
 * 초대(콘솔에서 이메일로 초대)는 가입 방식과 상관없이 항상 쓸 수 있다.
 */
public enum AppJoinPolicy {
    // 초대받은 사람만 들어온다. 앱 가입 페이지로는 가입할 수 없다 (기본값)
    INVITE_ONLY,
    // 앱 가입 페이지에서 신청하고, 관리자가 승인하면 사용자가 된다
    APPROVAL,
    // 앱 가입 페이지에서 가입하면 바로 사용자가 된다
    OPEN,
    ;

    public static AppJoinPolicy of(String value) {
        if(value == null || value.isBlank()) {
            return INVITE_ONLY;
        }
        for(AppJoinPolicy policy : values()) {
            if(policy.name().equals(value.trim())) {
                return policy;
            }
        }
        return null;
    }
}
