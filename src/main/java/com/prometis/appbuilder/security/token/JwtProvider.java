package com.prometis.appbuilder.security.token;

import com.prometis.appbuilder.security.domain.Client;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.exception.SecurityErrorMessage;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;

import java.security.Key;
import java.time.ZonedDateTime;
import java.util.Date;

public class JwtProvider {
    private final Key key;
    private final Long accessTokenExpireTime;
    private final Long refreshTokenExpireTime;

    public JwtProvider(String secretKey, long accessTokenExpireTime, long refreshTokenExpireTime) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpireTime = accessTokenExpireTime;
        this.refreshTokenExpireTime = refreshTokenExpireTime;
    }

    public String generateAccessToken(Long userId) {
        Claims claims = Jwts.claims();
        claims.put("userId", userId);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime tokenValidity = now.plusSeconds(accessTokenExpireTime);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now.toInstant()))
                .setExpiration(Date.from(tokenValidity.toInstant()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long parseAccessToken(String token) {
        Claims claims = parseClaims(token);

        return Long.valueOf(String.valueOf(claims.get("userId")));
    }

    public void validateToken(String token) throws SecurityBusinessException {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        }
        catch (SecurityException | MalformedJwtException e) {
            throw new SecurityBusinessException(SecurityErrorMessage.INVALID_TOKEN);
        }
        catch (ExpiredJwtException e) {
            throw new SecurityBusinessException(SecurityErrorMessage.EXPIRED_TOKEN);
        }
        catch (UnsupportedJwtException e) {
            throw new SecurityBusinessException(SecurityErrorMessage.UNSUPPORTED_TOKEN);
        }
        catch (IllegalArgumentException e) {
            throw new SecurityBusinessException(SecurityErrorMessage.EMPTY_TOKEN_CLAIMS);
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String generateRefreshToken(Long userId, Client client) {
        Claims claims = Jwts.claims();
        claims.put("userId", userId);
        claims.put("clientIp", client.getClientIp());
        claims.put("device", client.getDevice());

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime tokenValidity = now.plusSeconds(refreshTokenExpireTime);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now.toInstant()))
                .setExpiration(Date.from(tokenValidity.toInstant()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserIdToRefreshToken(String token) {
        Claims claims = parseClaims(token);

        return (Long) claims.get("userId");
    }
}
