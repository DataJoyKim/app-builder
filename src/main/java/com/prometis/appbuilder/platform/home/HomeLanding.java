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
    APPLICATION_MANAGE("/applications/manage"), // 소유한 애플리케이션 접속/생성 (첫 화면으로는 쓰지 않는다)
    APPLICATION_SELECT("/applications"),     // 플랫폼관리자가 아닌 사용자: 가입된 애플리케이션 선택
    ;

    private final String path;
}
