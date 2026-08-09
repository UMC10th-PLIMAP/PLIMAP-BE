package com.example.plimap.domain.auth.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plimap.auth.test-token")
public record TestTokenProperties(
        boolean issueKeyRequired,
        String issueKey
) {

    private static final int MINIMUM_ISSUE_KEY_BYTES = 32;

    public TestTokenProperties {
        issueKey = issueKey == null ? "" : issueKey;

        if (issueKeyRequired && issueKey.isBlank()) {
            throw new IllegalArgumentException("Dev 테스트 토큰 발급 키는 비어 있을 수 없습니다.");
        }
        if (issueKeyRequired && !issueKey.equals(issueKey.trim())) {
            throw new IllegalArgumentException("Dev 테스트 토큰 발급 키에는 앞뒤 공백을 사용할 수 없습니다.");
        }
        if (issueKeyRequired
                && issueKey.getBytes(StandardCharsets.UTF_8).length < MINIMUM_ISSUE_KEY_BYTES) {
            throw new IllegalArgumentException("Dev 테스트 토큰 발급 키는 32바이트 이상이어야 합니다.");
        }
    }

    public boolean matches(String candidate) {
        if (!issueKeyRequired) {
            return true;
        }
        if (candidate == null) {
            return false;
        }

        return MessageDigest.isEqual(
                issueKey.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8)
        );
    }
}
