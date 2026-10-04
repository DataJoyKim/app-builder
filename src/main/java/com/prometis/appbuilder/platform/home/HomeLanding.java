package com.prometis.appbuilder.platform.home;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 루트(/)로 들어온 로그인 사용자를 보낼 첫 화면.
 */
@Getter
@RequiredArgsConstructor
public enum HomeLanding {
    PLATFORM_CONSOLE("/console"),            // 플랫폼관리자
    APPLICATION_MANAGE("/applications/manage"), // 애플리케이션 관리자, 아직 애플리케이션이 없는 사용자: 소유한 애플리케이션 접속/생성
    APPLICATION_SELECT("/applications"),     // 일반 사용자: 가입된 애플리케이션 선택
    ;

    private final String path;
}
