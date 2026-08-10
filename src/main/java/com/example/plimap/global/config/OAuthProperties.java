package com.example.plimap.global.config;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

@ConfigurationProperties(prefix = "oauth")
public record OAuthProperties(
        String redirectUri,
        List<String> allowedFrontendOrigins
) {

    static final String PREVIEW_ORIGIN_PATTERN = CorsProperties.PREVIEW_ORIGIN_PATTERN;
    private static final String INVALID_WILDCARD_MESSAGE =
            "OAuth 허용 Origin에는 " + PREVIEW_ORIGIN_PATTERN + " 패턴 외 와일드카드를 사용할 수 없습니다.";
    private static final Pattern PREVIEW_ORIGIN = Pattern.compile(
            "^https://pr-[^.]+\\.plimap\\.kr$"
    );

    public OAuthProperties {
        redirectUri = normalizeRedirectUri(redirectUri);
        String defaultFrontendOrigin = extractOrigin(redirectUri);

        allowedFrontendOrigins = allowedFrontendOrigins == null
                ? List.of(defaultFrontendOrigin)
                : allowedFrontendOrigins.stream()
                        .map(OAuthProperties::normalizeAllowedOrigin)
                        .distinct()
                        .toList();

        if (allowedFrontendOrigins.isEmpty()) {
            allowedFrontendOrigins = List.of(defaultFrontendOrigin);
        }
        if (!allowedFrontendOrigins.contains(defaultFrontendOrigin)) {
            throw new IllegalArgumentException(
                    "OAuth 기본 리다이렉트 Origin은 허용된 프론트 Origin에 포함되어야 합니다."
            );
        }
    }

    public String defaultFrontendOrigin() {
        return extractOrigin(redirectUri);
    }

    public String requireAllowedFrontendOrigin(String origin) {
        String normalizedOrigin = normalizeOrigin(origin);
        if (allowedFrontendOrigins.stream()
                .noneMatch(allowedOrigin -> matchesAllowedOrigin(allowedOrigin, normalizedOrigin))) {
            throw new IllegalArgumentException("허용되지 않은 OAuth 프론트 Origin입니다.");
        }
        return normalizedOrigin;
    }

    public String redirectUriFor(String frontendOrigin) {
        String normalizedOrigin = requireAllowedFrontendOrigin(frontendOrigin);
        URI configuredRedirectUri = URI.create(redirectUri);

        return UriComponentsBuilder.fromUriString(normalizedOrigin)
                .path(configuredRedirectUri.getRawPath())
                .query(configuredRedirectUri.getRawQuery())
                .build(true)
                .toUriString();
    }

    static String normalizeOrigin(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuth 프론트 Origin은 비어 있을 수 없습니다.");
        }

        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("OAuth 프론트 Origin 형식이 올바르지 않습니다.", exception);
        }

        String scheme = uri.getScheme() == null
                ? ""
                : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null
                ? ""
                : uri.getHost().toLowerCase(Locale.ROOT);

        if ((!scheme.equals("http") && !scheme.equals("https"))
                || host.isBlank()
                || uri.getUserInfo() != null
                || (uri.getRawPath() != null
                    && !uri.getRawPath().isEmpty()
                    && !uri.getRawPath().equals("/"))
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "OAuth 프론트 Origin은 경로, query, fragment, credentials가 없는 HTTP(S) Origin이어야 합니다."
            );
        }

        if (scheme.equals("http") && !isLoopbackHost(host)) {
            throw new IllegalArgumentException("HTTP OAuth 프론트 Origin은 localhost에서만 허용됩니다.");
        }

        int port = uri.getPort();
        boolean isDefaultPort = port == -1
                || (scheme.equals("http") && port == 80)
                || (scheme.equals("https") && port == 443);
        String formattedHost = host.contains(":") ? "[" + host + "]" : host;

        return scheme + "://" + formattedHost + (isDefaultPort ? "" : ":" + port);
    }

    private static String normalizeAllowedOrigin(String value) {
        String trimmed = value == null ? null : value.trim();
        if (PREVIEW_ORIGIN_PATTERN.equals(trimmed)) {
            return PREVIEW_ORIGIN_PATTERN;
        }
        if (trimmed != null && trimmed.contains("*")) {
            throw new IllegalArgumentException(INVALID_WILDCARD_MESSAGE);
        }
        return normalizeOrigin(trimmed);
    }

    private static boolean matchesAllowedOrigin(String allowedOrigin, String requestedOrigin) {
        return PREVIEW_ORIGIN_PATTERN.equals(allowedOrigin)
                ? PREVIEW_ORIGIN.matcher(requestedOrigin).matches()
                : allowedOrigin.equals(requestedOrigin);
    }

    private static String normalizeRedirectUri(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuth 기본 리다이렉트 URI는 비어 있을 수 없습니다.");
        }

        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("OAuth 기본 리다이렉트 URI 형식이 올바르지 않습니다.", exception);
        }

        if (uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getRawFragment() != null
                || uri.getRawPath() == null
                || uri.getRawPath().isBlank()
                || uri.getRawPath().equals("/")) {
            throw new IllegalArgumentException(
                    "OAuth 기본 리다이렉트 URI에는 유효한 Origin과 프론트 경로가 필요합니다."
            );
        }

        String origin = normalizeOrigin(
                uri.getScheme() + "://" + uri.getRawAuthority()
        );
        return origin + uri.getRawPath()
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

    private static String extractOrigin(String redirectUri) {
        URI uri = URI.create(redirectUri);
        return normalizeOrigin(uri.getScheme() + "://" + uri.getRawAuthority());
    }

    private static boolean isLoopbackHost(String host) {
        return host.equals("localhost")
                || host.equals("127.0.0.1")
                || host.equals("::1");
    }
}
