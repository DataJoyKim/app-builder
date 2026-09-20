package com.prometis.appbuilder.security.ip;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * 허용 IP 표기와 접속 IP 를 비교한다. 지원하는 표기는 네 가지다.
 * 전체       : *              (모든 IP 허용)
 * 단일       : 192.168.0.10, ::1
 * 대역(CIDR) : 192.168.0.0/24, 2001:db8::/32
 * 와일드카드 : 192.168.0.*, 192.168.*.*  (IPv4 만)
 *
 * 호스트명은 DNS 조회가 필요해 받지 않는다. (IP 리터럴만 비교한다)
 */
@Slf4j
public class IpAddressMatcher {
    public static final String ALL = "*";

    private static final Pattern IPV4_LITERAL = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern IPV4_WILDCARD = Pattern.compile("^(\\d{1,3}|\\*)(\\.(\\d{1,3}|\\*)){3}$");

    private IpAddressMatcher() {
    }

    public static boolean matches(String pattern, String clientIp) {
        if(pattern == null || pattern.isBlank() || clientIp == null || clientIp.isBlank()) {
            return false;
        }

        String target = pattern.trim();

        if(ALL.equals(target)) {
            return true;
        }

        if(target.contains("/")) {
            return matchesCidr(target, clientIp);
        }

        if(target.contains(ALL)) {
            return matchesWildcard(target, clientIp);
        }

        return matchesAddress(target, clientIp);
    }

    // 표기가 IP/대역/와일드카드 중 하나로 읽히는지. (콘솔에서 저장 전에 확인하는 용도)
    public static boolean isValidPattern(String pattern) {
        if(pattern == null || pattern.isBlank()) {
            return false;
        }

        String target = pattern.trim();

        if(ALL.equals(target)) {
            return true;
        }

        if(target.contains("/")) {
            return parseCidr(target) != null;
        }

        if(target.contains(ALL)) {
            return IPV4_WILDCARD.matcher(target).matches();
        }

        return parseAddress(target) != null;
    }

    private static boolean matchesAddress(String pattern, String clientIp) {
        InetAddress patternAddress = parseAddress(pattern);
        InetAddress clientAddress = parseAddress(clientIp);

        if(patternAddress == null || clientAddress == null) {
            return false;
        }

        return patternAddress.equals(clientAddress);
    }

    private static boolean matchesWildcard(String pattern, String clientIp) {
        InetAddress clientAddress = parseAddress(clientIp);

        if(clientAddress == null || !IPV4_WILDCARD.matcher(pattern).matches()) {
            return false;
        }

        // IPv4 로 표기한 와일드카드는 IPv4 접속에만 적용한다.
        String client = clientAddress.getHostAddress();
        if(!IPV4_LITERAL.matcher(client).matches()) {
            return false;
        }

        String[] patternParts = pattern.split("\\.");
        String[] clientParts = client.split("\\.");

        for(int index = 0; index < patternParts.length; index++) {
            if(ALL.equals(patternParts[index])) {
                continue;
            }

            if(!patternParts[index].equals(clientParts[index])) {
                return false;
            }
        }

        return true;
    }

    private static boolean matchesCidr(String pattern, String clientIp) {
        Cidr cidr = parseCidr(pattern);
        InetAddress clientAddress = parseAddress(clientIp);

        if(cidr == null || clientAddress == null) {
            return false;
        }

        byte[] network = cidr.address().getAddress();
        byte[] client = clientAddress.getAddress();

        // IPv4 대역과 IPv6 접속처럼 체계가 다르면 비교하지않는다.
        if(network.length != client.length) {
            return false;
        }

        int fullBytes = cidr.prefixLength() / 8;
        int remainBits = cidr.prefixLength() % 8;

        for(int index = 0; index < fullBytes; index++) {
            if(network[index] != client[index]) {
                return false;
            }
        }

        if(remainBits == 0) {
            return true;
        }

        int mask = (0xFF << (8 - remainBits)) & 0xFF;

        return (network[fullBytes] & mask) == (client[fullBytes] & mask);
    }

    private static Cidr parseCidr(String pattern) {
        String[] parts = pattern.split("/");
        if(parts.length != 2) {
            return null;
        }

        InetAddress address = parseAddress(parts[0]);
        if(address == null) {
            return null;
        }

        int prefixLength;
        try {
            prefixLength = Integer.parseInt(parts[1].trim());
        }
        catch (NumberFormatException e) {
            return null;
        }

        if(prefixLength < 0 || prefixLength > address.getAddress().length * 8) {
            return null;
        }

        return new Cidr(address, prefixLength);
    }

    // 호스트명(DNS 조회)은 받지 않고 IP 리터럴만 읽는다.
    private static InetAddress parseAddress(String value) {
        if(value == null || value.isBlank()) {
            return null;
        }

        String target = value.trim();

        // IPv6 는 [::1] 처럼 대괄호로 올 수 있다.
        if(target.startsWith("[") && target.endsWith("]")) {
            target = target.substring(1, target.length() - 1);
        }

        if(!IPV4_LITERAL.matcher(target).matches() && target.indexOf(':') < 0) {
            return null;
        }

        try {
            return InetAddress.getByName(target);
        }
        catch (UnknownHostException e) {
            log.debug("IP 형식이 올바르지않습니다. [{}]", value);
            return null;
        }
    }

    private record Cidr(InetAddress address, int prefixLength) {
    }
}
