package com.prometis.appbuilder.security.domainaccess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 허용 도메인 표기(정확히/와일드카드/전체)와 접근 도메인 비교, Referer 에서 도메인 꺼내기를 확인한다.
 */
class DomainMatcherTest {
    @Test
    void 정확한_도메인은_같을때만_통과한다() {
        assertTrue(DomainMatcher.matches("portal.example.com", "portal.example.com"));
        assertFalse(DomainMatcher.matches("portal.example.com", "admin.example.com"));
        assertFalse(DomainMatcher.matches("example.com", "notexample.com"));
    }

    @Test
    void 대소문자와_포트_끝점은_무시한다() {
        assertTrue(DomainMatcher.matches("Portal.Example.com", "portal.example.COM"));
        assertTrue(DomainMatcher.matches("portal.example.com", "portal.example.com:8080"));
        assertTrue(DomainMatcher.matches("portal.example.com:8080", "portal.example.com"));
        assertTrue(DomainMatcher.matches("portal.example.com", "portal.example.com."));
    }

    @Test
    void 와일드카드는_하위도메인을_허용한다() {
        assertTrue(DomainMatcher.matches("*.example.com", "portal.example.com"));
        assertTrue(DomainMatcher.matches("*.example.com", "a.b.example.com"));

        // 하위 도메인만 허용하고 example.com 자신은 따로 등록해야 한다.
        assertFalse(DomainMatcher.matches("*.example.com", "example.com"));
        assertFalse(DomainMatcher.matches("*.example.com", "example.com.attacker.com"));
        assertFalse(DomainMatcher.matches("*.example.com", "myexample.com"));
    }

    @Test
    void 이름_일부만_바꾸는_와일드카드도_쓸수있다() {
        assertTrue(DomainMatcher.matches("dev-*.example.com", "dev-01.example.com"));
        assertFalse(DomainMatcher.matches("dev-*.example.com", "prod-01.example.com"));
        // 단계를 넘어가지는 않는다.
        assertFalse(DomainMatcher.matches("dev-*.example.com", "dev-a.b.example.com"));
    }

    @Test
    void 전체허용과_빈값() {
        assertTrue(DomainMatcher.matches("*", "example.com"));
        assertTrue(DomainMatcher.matches("*", "localhost"));

        assertFalse(DomainMatcher.matches(null, "example.com"));
        assertFalse(DomainMatcher.matches("example.com", null));
        assertFalse(DomainMatcher.matches("  ", "example.com"));
    }

    @Test
    void Referer_에서_도메인만_꺼낸다() {
        assertEquals("portal.example.com", DomainMatcher.hostOf("https://portal.example.com/board/list?page=1"));
        assertEquals("portal.example.com", DomainMatcher.hostOf("http://portal.example.com:8080/"));
        assertEquals("portal.example.com", DomainMatcher.hostOf("https://user:pw@portal.example.com/x"));
        assertEquals("localhost", DomainMatcher.hostOf("http://localhost:8080/console"));
        assertEquals("example.com", DomainMatcher.hostOf("example.com"));
        assertEquals("::1", DomainMatcher.hostOf("http://[::1]:8080/page"));

        assertNull(DomainMatcher.hostOf(null));
        assertNull(DomainMatcher.hostOf(""));
        assertNull(DomainMatcher.hostOf("https://"));
    }

    @Test
    void 저장전_형식검사() {
        assertTrue(DomainMatcher.isValidPattern("*"));
        assertTrue(DomainMatcher.isValidPattern("portal.example.com"));
        assertTrue(DomainMatcher.isValidPattern("*.example.com"));
        assertTrue(DomainMatcher.isValidPattern("dev-*.example.com"));
        assertTrue(DomainMatcher.isValidPattern("localhost"));

        assertFalse(DomainMatcher.isValidPattern(null));
        assertFalse(DomainMatcher.isValidPattern(" "));
        assertFalse(DomainMatcher.isValidPattern("example..com"));
        assertFalse(DomainMatcher.isValidPattern("https://example.com"));
        assertFalse(DomainMatcher.isValidPattern("example.com/board"));
        assertFalse(DomainMatcher.isValidPattern("사내 포털"));
    }
}
