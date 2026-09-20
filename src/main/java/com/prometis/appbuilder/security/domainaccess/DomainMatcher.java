package com.prometis.appbuilder.security.domainaccess;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 허용 도메인 표기와 접근한 도메인을 비교한다. 지원하는 표기는 세 가지다.
 * 전체       : *                     (모든 도메인 허용)
 * 정확히     : portal.example.com
 * 와일드카드 : *.example.com          (example.com 의 하위 도메인 전체. example.com 자신은 포함하지않는다)
 *             dev-*.example.com      (한 단계 이름의 일부만 바꿔가며 허용)
 *
 * 대소문자는 구분하지않고, 포트(:8080)와 끝의 점은 떼고 비교한다.
 */
public class DomainMatcher {
    public static final String ALL = "*";

    private static final Pattern DOMAIN_CHARS = Pattern.compile("^[A-Za-z0-9*._-]+$");

    private DomainMatcher() {
    }

    public static boolean matches(String pattern, String domain) {
        String target = normalize(pattern);
        String host = normalize(domain);

        if(target == null || host == null) {
            return false;
        }

        if(ALL.equals(target)) {
            return true;
        }

        if(!target.contains(ALL)) {
            return target.equals(host);
        }

        return Pattern.compile(toRegex(target)).matcher(host).matches();
    }

    // 표기가 도메인/와일드카드로 읽히는지. (콘솔에서 저장 전에 확인하는 용도)
    public static boolean isValidPattern(String pattern) {
        if(pattern == null || pattern.isBlank()) {
            return false;
        }

        String raw = pattern.trim().toLowerCase(Locale.ROOT);

        if(ALL.equals(raw)) {
            return true;
        }

        // 포트를 붙여 적는 것까지는 받아주되, 그 뒤가 숫자가 아니면(https://... 등) 도메인 표기로 보지않는다.
        int colon = raw.indexOf(':');
        if(colon >= 0) {
            if(!raw.substring(colon + 1).matches("\\d+")) {
                return false;
            }

            raw = raw.substring(0, colon);
        }

        String target = normalize(raw);

        if(target == null) {
            return false;
        }

        if(!DOMAIN_CHARS.matcher(target).matches()) {
            return false;
        }

        // 점으로만 되어있거나 빈 단계(example..com)가 있으면 도메인으로 보지않는다.
        for(String label : target.split("\\.", -1)) {
            if(label.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 요청 헤더(Referer/Origin)의 URL 에서 도메인만 꺼낸다. 도메인만 들어있는 값도 그대로 받는다.
     */
    public static String hostOf(String url) {
        if(url == null || url.isBlank()) {
            return null;
        }

        String value = url.trim();

        int schemeIndex = value.indexOf("://");
        if(schemeIndex >= 0) {
            value = value.substring(schemeIndex + 3);
        }

        // 사용자정보(user@host)와 경로/쿼리/프래그먼트를 떼어낸다.
        int at = value.indexOf('@');
        if(at >= 0) {
            value = value.substring(at + 1);
        }

        int end = value.length();
        for(char delimiter : new char[]{'/', '?', '#'}) {
            int index = value.indexOf(delimiter);
            if(index >= 0 && index < end) {
                end = index;
            }
        }
        value = value.substring(0, end);

        // IPv6 는 [::1]:8080 형태로 온다.
        if(value.startsWith("[")) {
            int close = value.indexOf(']');
            return (close > 0) ? value.substring(1, close) : null;
        }

        int colon = value.indexOf(':');
        if(colon >= 0) {
            value = value.substring(0, colon);
        }

        return value.isEmpty() ? null : value;
    }

    private static String normalize(String value) {
        if(value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        // 포트는 비교하지않는다.
        if(!normalized.startsWith("[")) {
            int colon = normalized.indexOf(':');
            if(colon >= 0) {
                normalized = normalized.substring(0, colon);
            }
        }

        // 끝의 점(루트 도메인 표기)은 떼어낸다.
        while(normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * '*.example.com' 은 하위 도메인 몇 단계든 허용하고, 단계 안의 '*'(dev-*.example.com)는 그 단계에서만 바뀐다.
     */
    private static String toRegex(String pattern) {
        StringBuilder regex = new StringBuilder();

        String remain = pattern;

        if(remain.startsWith("*.")) {
            regex.append("(?:[^.]+\\.)+");
            remain = remain.substring(2);
        }

        for(char c : remain.toCharArray()) {
            if(c == '*') {
                regex.append("[^.]*");
            }
            else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }

        return regex.toString();
    }
}
