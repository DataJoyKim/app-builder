package com.prometis.appbuilder.app.security.ip;

import com.prometis.appbuilder.app.security.ip.IpAddressMatcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 허용 IP 표기(단일/대역/와일드카드/전체)와 접속 IP 비교를 확인한다.
 */
class IpAddressMatcherTest {
    @Test
    void 단일_IP는_같을때만_통과한다() {
        assertTrue(IpAddressMatcher.matches("192.168.0.10", "192.168.0.10"));
        assertFalse(IpAddressMatcher.matches("192.168.0.10", "192.168.0.11"));

        // 앞에 0 이 붙거나 IPv4 로 바뀌는 IPv6 표기도 같은 주소로 본다.
        assertTrue(IpAddressMatcher.matches("192.168.0.10", "::ffff:192.168.0.10"));
    }

    @Test
    void 대역은_CIDR_범위_안이면_통과한다() {
        assertTrue(IpAddressMatcher.matches("192.168.0.0/24", "192.168.0.1"));
        assertTrue(IpAddressMatcher.matches("192.168.0.0/24", "192.168.0.255"));
        assertFalse(IpAddressMatcher.matches("192.168.0.0/24", "192.168.1.1"));

        // 8비트로 안 떨어지는 프리픽스
        assertTrue(IpAddressMatcher.matches("10.0.0.0/12", "10.15.255.1"));
        assertFalse(IpAddressMatcher.matches("10.0.0.0/12", "10.16.0.1"));

        assertTrue(IpAddressMatcher.matches("0.0.0.0/0", "8.8.8.8"));
    }

    @Test
    void 와일드카드는_자리수가_맞으면_통과한다() {
        assertTrue(IpAddressMatcher.matches("192.168.0.*", "192.168.0.77"));
        assertFalse(IpAddressMatcher.matches("192.168.0.*", "192.168.1.77"));
        assertTrue(IpAddressMatcher.matches("192.168.*.*", "192.168.9.77"));
        assertTrue(IpAddressMatcher.matches("*", "8.8.8.8"));
        assertTrue(IpAddressMatcher.matches("*", "::1"));
    }

    @Test
    void IPv6도_단일과_대역을_비교한다() {
        assertTrue(IpAddressMatcher.matches("::1", "0:0:0:0:0:0:0:1"));
        assertTrue(IpAddressMatcher.matches("2001:db8::/32", "2001:db8:1234::1"));
        assertFalse(IpAddressMatcher.matches("2001:db8::/32", "2001:db9::1"));

        // 체계가 다르면 비교하지않는다.
        assertFalse(IpAddressMatcher.matches("192.168.0.0/24", "::1"));
        assertFalse(IpAddressMatcher.matches("192.168.0.*", "::1"));
    }

    @Test
    void 형식이_잘못되었거나_비어있으면_통과하지않는다() {
        assertFalse(IpAddressMatcher.matches(null, "192.168.0.1"));
        assertFalse(IpAddressMatcher.matches("192.168.0.1", null));
        assertFalse(IpAddressMatcher.matches("", "192.168.0.1"));
        assertFalse(IpAddressMatcher.matches("192.168.0.1/40", "192.168.0.1"));
        assertFalse(IpAddressMatcher.matches("192.168.0.0/abc", "192.168.0.1"));

        // 호스트명은 DNS 조회를 하지않으므로 통과하지않는다.
        assertFalse(IpAddressMatcher.matches("localhost", "127.0.0.1"));
    }

    @Test
    void 저장전_형식검사() {
        assertTrue(IpAddressMatcher.isValidPattern("*"));
        assertTrue(IpAddressMatcher.isValidPattern("192.168.0.10"));
        assertTrue(IpAddressMatcher.isValidPattern("192.168.0.0/24"));
        assertTrue(IpAddressMatcher.isValidPattern("192.168.*.*"));
        assertTrue(IpAddressMatcher.isValidPattern("2001:db8::/32"));

        assertFalse(IpAddressMatcher.isValidPattern(null));
        assertFalse(IpAddressMatcher.isValidPattern(" "));
        assertFalse(IpAddressMatcher.isValidPattern("192.168.0"));
        assertFalse(IpAddressMatcher.isValidPattern("192.168.0.0/33"));
        assertFalse(IpAddressMatcher.isValidPattern("사내망"));
    }
}
