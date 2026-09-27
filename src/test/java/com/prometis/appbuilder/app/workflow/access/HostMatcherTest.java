package com.prometis.appbuilder.app.workflow.access;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 허용 도메인 표기(정확히/와일드카드/전체)와 접근 도메인 비교, Referer 에서 도메인 꺼내기를 확인한다.
 */
class HostMatcherTest {
    @Test
    void 정확한_도메인은_같을때만_통과한다() {
        assertTrue(HostMatcher.matches("portal.example.com", "portal.example.com"));
        assertFalse(HostMatcher.matches("portal.example.com", "admin.example.com"));
        assertFalse(HostMatcher.matches("example.com", "notexample.com"));
    }

    @Test
    void 대소문자와_포트_끝점은_무시한다() {
        assertTrue(HostMatcher.matches("Portal.Example.com", "portal.example.COM"));
        assertTrue(HostMatcher.matches("portal.example.com", "portal.example.com:8080"));
        assertTrue(HostMatcher.matches("portal.example.com:8080", "portal.example.com"));
        assertTrue(HostMatcher.matches("portal.example.com", "portal.example.com."));
    }

    @Test
    void 와일드카드는_하위도메인을_허용한다() {
        assertTrue(HostMatcher.matches("*.example.com", "portal.example.com"));
        assertTrue(HostMatcher.matches("*.example.com", "a.b.example.com"));

        // 하위 도메인만 허용하고 example.com 자신은 따로 등록해야 한다.
        assertFalse(HostMatcher.matches("*.example.com", "example.com"));
        assertFalse(HostMatcher.matches("*.example.com", "example.com.attacker.com"));
        assertFalse(HostMatcher.matches("*.example.com", "myexample.com"));
    }

    @Test
    void 이름_일부만_바꾸는_와일드카드도_쓸수있다() {
        assertTrue(HostMatcher.matches("dev-*.example.com", "dev-01.example.com"));
        assertFalse(HostMatcher.matches("dev-*.example.com", "prod-01.example.com"));
        // 단계를 넘어가지는 않는다.
        assertFalse(HostMatcher.matches("dev-*.example.com", "dev-a.b.example.com"));
    }

    @Test
    void 전체허용과_빈값() {
        assertTrue(HostMatcher.matches("*", "example.com"));
        assertTrue(HostMatcher.matches("*", "localhost"));

        assertFalse(HostMatcher.matches(null, "example.com"));
        assertFalse(HostMatcher.matches("example.com", null));
        assertFalse(HostMatcher.matches("  ", "example.com"));
    }

    @Test
    void Referer_에서_도메인만_꺼낸다() {
        assertEquals("portal.example.com", HostMatcher.hostOf("https://portal.example.com/board/list?page=1"));
        assertEquals("portal.example.com", HostMatcher.hostOf("http://portal.example.com:8080/"));
        assertEquals("portal.example.com", HostMatcher.hostOf("https://user:pw@portal.example.com/x"));
        assertEquals("localhost", HostMatcher.hostOf("http://localhost:8080/console"));
        assertEquals("example.com", HostMatcher.hostOf("example.com"));
        assertEquals("::1", HostMatcher.hostOf("http://[::1]:8080/page"));

        assertNull(HostMatcher.hostOf(null));
        assertNull(HostMatcher.hostOf(""));
        assertNull(HostMatcher.hostOf("https://"));
    }

    @Test
    void 저장전_형식검사() {
        assertTrue(HostMatcher.isValidPattern("*"));
        assertTrue(HostMatcher.isValidPattern("portal.example.com"));
        assertTrue(HostMatcher.isValidPattern("*.example.com"));
        assertTrue(HostMatcher.isValidPattern("dev-*.example.com"));
        assertTrue(HostMatcher.isValidPattern("localhost"));

        assertFalse(HostMatcher.isValidPattern(null));
        assertFalse(HostMatcher.isValidPattern(" "));
        assertFalse(HostMatcher.isValidPattern("example..com"));
        assertFalse(HostMatcher.isValidPattern("https://example.com"));
        assertFalse(HostMatcher.isValidPattern("example.com/board"));
        assertFalse(HostMatcher.isValidPattern("사내 포털"));
    }
}
