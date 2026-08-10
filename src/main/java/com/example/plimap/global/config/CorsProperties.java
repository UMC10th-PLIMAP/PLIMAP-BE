package com.example.plimap.global.config;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plimap.cors")
public record CorsProperties(List<String> allowedOrigins) {

    static final String PREVIEW_ORIGIN_PATTERN = "https://pr-*.plimap.kr";
    static final Pattern PREVIEW_ORIGIN = Pattern.compile(
            "^https://pr-[^.]+\\.plimap\\.kr$",
            Pattern.CASE_INSENSITIVE
    );
    static final String PRIVATE_NETWORK_HTTP_ORIGIN_PATTERN = "http://192.168.*:[*]";
    static final String PRIVATE_NETWORK_HTTPS_ORIGIN_PATTERN = "https://192.168.*:[*]";
    static final String INVALID_WILDCARD_MESSAGE =
            "CORS 허용 Origin에는 "
                    + PREVIEW_ORIGIN_PATTERN + ", "
                    + PRIVATE_NETWORK_HTTP_ORIGIN_PATTERN + ", "
                    + PRIVATE_NETWORK_HTTPS_ORIGIN_PATTERN
                    + " 패턴 외 와일드카드를 사용할 수 없습니다.";

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList();

        if (allowedOrigins.stream()
                .anyMatch(origin -> origin.contains("*") && !isAllowedWildcardPattern(origin))) {
            throw new IllegalArgumentException(INVALID_WILDCARD_MESSAGE);
        }
    }

    static boolean isPrivateNetworkOriginPattern(String origin) {
        return PRIVATE_NETWORK_HTTP_ORIGIN_PATTERN.equals(origin)
                || PRIVATE_NETWORK_HTTPS_ORIGIN_PATTERN.equals(origin);
    }

    static String privateNetworkOriginScheme(String origin) {
        if (PRIVATE_NETWORK_HTTP_ORIGIN_PATTERN.equals(origin)) {
            return "http";
        }
        if (PRIVATE_NETWORK_HTTPS_ORIGIN_PATTERN.equals(origin)) {
            return "https";
        }
        return null;
    }

    private static boolean isAllowedWildcardPattern(String origin) {
        return PREVIEW_ORIGIN_PATTERN.equals(origin) || isPrivateNetworkOriginPattern(origin);
    }
}
