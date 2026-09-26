package com.prometis.appbuilder.app.security.ip;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 접속 IP 를 찾는다. 프록시/로드밸런서를 거치면 실제 접속 IP 가 헤더에 담겨오므로 헤더를 먼저 본다.
 * X-Forwarded-For 는 '클라이언트, 프록시1, 프록시2' 형태라 맨 앞 값이 실제 접속 IP 다.
 */
public class ClientIpResolver {
    private static final List<String> HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if(request == null) {
            return null;
        }

        for(String header : HEADERS) {
            String value = firstAddressOf(request.getHeader(header));

            if(value != null) {
                return value;
            }
        }

        return normalize(request.getRemoteAddr());
    }

    private static String firstAddressOf(String headerValue) {
        if(headerValue == null || headerValue.isBlank() || "unknown".equalsIgnoreCase(headerValue.trim())) {
            return null;
        }

        String first = headerValue.split(",")[0].trim();

        return first.isEmpty() ? null : normalize(first);
    }

    private static String normalize(String ip) {
        if(ip == null || ip.isBlank()) {
            return null;
        }

        String value = ip.trim();

        // IPv6 는 [::1]:port 또는 [::1] 로 올 수 있다.
        if(value.startsWith("[")) {
            int end = value.indexOf(']');
            return (end > 0) ? value.substring(1, end) : value;
        }

        // IPv4 는 1.2.3.4:port 로 올 수 있다. (IPv6 는 ':' 가 여러 개라 건드리지않는다)
        int colon = value.indexOf(':');
        if(colon > 0 && value.indexOf(':', colon + 1) < 0) {
            return value.substring(0, colon);
        }

        return value;
    }
}
