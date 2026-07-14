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

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AuthMember authMember) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(authMember.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY.toMillis()))
                .signWith(secretKey)
                .compact();
    }

    public Duration getAccessTokenExpiry() {
        return ACCESS_TOKEN_EXPIRY;
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

    public Duration getRemainingExpiry(String token) {
        Instant expiration = parseToken(token).getExpiration().toInstant();
        return Duration.between(Instant.now(), expiration);
    }
}
