package com.prometis.appbuilder.security.token;

import com.prometis.core.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TokenCookie {
    private static final String ACCESS_TOKEN_NAME = "accessToken";
    private static final String REFRESH_TOKEN_NAME = "refreshToken";

    public static String resolveAccessToken(HttpServletRequest request) {
        Cookie cookie = CookieUtil.getCookie(request,ACCESS_TOKEN_NAME);
        if(cookie == null) {
            return null;
        }

        return cookie.getValue();
    }

    public static String resolveRefreshToken(HttpServletRequest request) {
        Cookie cookie = CookieUtil.getCookie(request,REFRESH_TOKEN_NAME);
        if(cookie == null) {
            return null;
        }

        return cookie.getValue();
    }

    public static void setAccessToken(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        //cookie.setMaxAge(3600);
        //cookie.setDomain("yourdomain.com");

        response.addCookie(cookie);
    }

    public static void setRefreshToken(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        //cookie.setMaxAge(3600*24);
        //cookie.setDomain("yourdomain.com");

        response.addCookie(cookie);
    }

    // 로그아웃: 같은 이름/경로의 쿠키를 즉시 만료시켜 덮어쓴다
    public static void clear(HttpServletResponse response) {
        for(String name : new String[]{ACCESS_TOKEN_NAME, REFRESH_TOKEN_NAME}) {
            Cookie cookie = new Cookie(name, "");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);

            response.addCookie(cookie);
        }
    }
}
