package com.example.plimap.global.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
                || nonStandardPort
                || isLoopbackHost(host)
                || path == null
                || !path.matches("^/[^/]+$")
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("Prod database URL is invalid.");
        }

        List<QueryParameter> queryParameters = parseQueryParameters(uri.getRawQuery());
        if (uri.getUserInfo() != null || containsCredentialQuery(queryParameters)) {
            throw new IllegalArgumentException(
                    "Prod database URL must not contain credentials."
            );
        }
        if (!hasRequiredSslMode(queryParameters)) {
            throw new IllegalArgumentException(
                    "Prod database URL must use sslmode=require."
            );
        }
    }

    private static boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host)
                || host.matches("^127(?:\\.[0-9]{1,3}){3}$")
                || "::1".equals(host)
                || "[::1]".equals(host);
    }

    private static List<QueryParameter> parseQueryParameters(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return List.of();
        }

        List<QueryParameter> parameters = new ArrayList<>();
        try {
            for (String parameter : rawQuery.split("&", -1)) {
                String[] parts = parameter.split("=", 2);
                if (parts[0].isBlank()) {
                    throw new IllegalArgumentException();
                }
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length == 2
                        ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                        : "";
                parameters.add(new QueryParameter(key, value));
            }
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Prod database URL is invalid.");
        }
        return List.copyOf(parameters);
    }

    private static boolean containsCredentialQuery(List<QueryParameter> queryParameters) {
        return queryParameters.stream()
                .map(QueryParameter::key)
                .anyMatch(key -> "user".equalsIgnoreCase(key)
                        || "password".equalsIgnoreCase(key));
    }

    private static boolean hasRequiredSslMode(List<QueryParameter> queryParameters) {
        List<String> sslModes = queryParameters.stream()
                .filter(parameter -> "sslmode".equalsIgnoreCase(parameter.key()))
                .map(QueryParameter::value)
                .toList();
        return sslModes.size() == 1 && "require".equalsIgnoreCase(sslModes.getFirst());
    }

    private record QueryParameter(String key, String value) {
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
