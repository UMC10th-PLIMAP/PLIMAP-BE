package com.example.plimap.global.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProdRuntimeValidationConfig {

    ProdRuntimeValidationConfig(
            @Value("${spring.data.redis.url}") String redisUrl,
            @Value("${spring.datasource.url}") String databaseUrl,
            @Value("${spring.datasource.username}") String databaseUsername,
            @Value("${spring.flyway.user}") String flywayUsername,
            @Value("${jwt.secret}") String jwtSecret
    ) {
        validateRedisUrl(redisUrl);
        validateDatabaseUrl(databaseUrl);
        validateDatabaseUsers(databaseUsername, flywayUsername);
        validateJwtSecret(jwtSecret);
    }

    static void validateRedisUrl(String redisUrl) {
        if (redisUrl == null || redisUrl.isBlank()) {
            throw new IllegalArgumentException("Prod Redis URL is required.");
        }

        final URI uri;
        try {
            uri = URI.create(redisUrl.trim());
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Prod Redis URL is invalid.");
        }

        if (!"rediss".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getHost().isBlank()) {
            throw new IllegalArgumentException(
                    "Prod Redis URL must use rediss:// with a host."
            );
        }
    }

    static void validateDatabaseUrl(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("Prod database URL is required.");
        }

        String normalizedUrl = databaseUrl.trim();
        if (!normalizedUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("Prod database URL must use PostgreSQL JDBC.");
        }

        final URI uri;
        try {
            uri = URI.create(normalizedUrl.substring("jdbc:".length()));
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Prod database URL is invalid.");
        }

        String path = uri.getPath();
        String host = uri.getHost();
        boolean nonStandardPort = uri.getPort() != -1 && uri.getPort() != 5432;
        if (!"postgresql".equalsIgnoreCase(uri.getScheme())
                || host == null
                || host.isBlank()
                || (nonStandardPort && !isLoopbackHost(host))
                || path == null
                || !path.matches("^/[^/]+$")
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("Prod database URL is invalid.");
        }

        if (uri.getUserInfo() != null || containsCredentialQuery(uri.getRawQuery())) {
            throw new IllegalArgumentException(
                    "Prod database URL must not contain credentials."
            );
        }
    }

    private static boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
    }

    private static boolean containsCredentialQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return false;
        }
        for (String parameter : rawQuery.split("&")) {
            String key = parameter.split("=", 2)[0];
            if ("user".equalsIgnoreCase(key) || "password".equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    static void validateDatabaseUsers(String databaseUsername, String flywayUsername) {
        if (!"plimap_app".equals(databaseUsername)) {
            throw new IllegalArgumentException("Prod database runtime user is invalid.");
        }
        if (!"plimap_migrator".equals(flywayUsername)) {
            throw new IllegalArgumentException("Prod Flyway user is invalid.");
        }
    }

    static void validateJwtSecret(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException("Prod JWT secret is required.");
        }
        if (!jwtSecret.equals(jwtSecret.trim())) {
            throw new IllegalArgumentException(
                    "Prod JWT secret must not have surrounding whitespace."
            );
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("Prod JWT secret must be at least 32 bytes.");
        }
    }
}
