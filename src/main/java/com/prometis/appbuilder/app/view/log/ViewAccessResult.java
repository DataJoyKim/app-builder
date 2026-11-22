package com.prometis.appbuilder.app.view.log;

/**
 * 화면 접근 결과.
 * - ALLOWED         : 화면을 열었다
 * - UNAUTHENTICATED : 로그인이 필요해 로그인 화면으로 보냈다 (401)
 * - FORBIDDEN       : 애플리케이션 사용자가 아니거나 권한이 없어 막았다 (403)
 */
public enum ViewAccessResult {
    ALLOWED, UNAUTHENTICATED, FORBIDDEN
}
