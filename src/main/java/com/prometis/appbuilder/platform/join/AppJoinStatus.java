package com.prometis.appbuilder.platform.join;

/**
 * 한 사용자의 애플리케이션 가입 상태 (앱 가입 페이지가 무엇을 보여줄지 정한다).
 */
public enum AppJoinStatus {
    // 이미 애플리케이션 사용자(또는 관리자)
    MEMBER,
    // 가입 신청을 하고 승인을 기다리는 중
    PENDING,
    // 아직 가입하지 않음
    NONE,
}
