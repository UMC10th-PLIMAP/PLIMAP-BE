package com.example.plimap.global.security;

import com.example.plimap.domain.auth.entity.AuthMember;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Duration ACCESS_TOKEN_EXPIRY = Duration.ofDays(1);
    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(14);
    private static final Duration TEST_ACCESS_TOKEN_EXPIRY = Duration.ofHours(1);

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AuthMember authMember) {
        return createToken(authMember, TOKEN_TYPE_ACCESS, ACCESS_TOKEN_EXPIRY);
    }

    public String createTestAccessToken(AuthMember authMember) {
        return createToken(authMember, TOKEN_TYPE_ACCESS, TEST_ACCESS_TOKEN_EXPIRY);
    }

    public String createRefreshToken(AuthMember authMember) {
        return createToken(authMember, TOKEN_TYPE_REFRESH, REFRESH_TOKEN_EXPIRY);
    }

    public Duration getAccessTokenExpiry() {
        return ACCESS_TOKEN_EXPIRY;
    }

    public Duration getRefreshTokenExpiry() {
        return REFRESH_TOKEN_EXPIRY;
    }

    public Duration getTestAccessTokenExpiry() {
        return TEST_ACCESS_TOKEN_EXPIRY;
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getMemberId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public String getJti(String token) {
        return parseToken(token).getId();
    }

    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(parseToken(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(parseToken(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    public Duration getRemainingExpiry(String token) {
        Instant expiration = parseToken(token).getExpiration().toInstant();
        Duration remaining = Duration.between(Instant.now(), expiration);
        return remaining.isPositive() ? remaining : Duration.ofMillis(1);
    }

    private String createToken(
            AuthMember authMember,
            String tokenType,
            Duration expiry
    ) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(authMember.getUsername())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry.toMillis()))
                .signWith(secretKey)
                .compact();
    }
}
