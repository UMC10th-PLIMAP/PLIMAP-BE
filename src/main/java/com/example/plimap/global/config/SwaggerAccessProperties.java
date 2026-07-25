package com.example.plimap.global.config;

import java.net.URI;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plimap.swagger.access")
public record SwaggerAccessProperties(String publicOrigin) {

    public SwaggerAccessProperties {
        if (publicOrigin == null || publicOrigin.isBlank()) {
            throw new IllegalArgumentException("Swagger 공개 Origin은 비어 있을 수 없습니다.");
        }

        URI uri;
        try {
            uri = URI.create(publicOrigin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Swagger 공개 Origin 형식이 올바르지 않습니다.", exception);
        }

        String scheme = uri.getScheme() == null
                ? ""
                : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null
                ? ""
                : uri.getHost().toLowerCase(Locale.ROOT);
        boolean hasPath = uri.getRawPath() != null
                && !uri.getRawPath().isEmpty()
                && !uri.getRawPath().equals("/");
        boolean usesDefaultPort = uri.getPort() == -1 || uri.getPort() == 443;

        if (!scheme.equals("https")
                || host.isBlank()
                || uri.getUserInfo() != null
                || hasPath
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || !usesDefaultPort) {
            throw new IllegalArgumentException(
                    "Swagger 공개 Origin은 경로, query, fragment, credentials, custom port가 없는 HTTPS Origin이어야 합니다."
            );
        }

        publicOrigin = "https://" + host;
    }

    public String allowedHost() {
        return URI.create(publicOrigin).getHost();
    }
}
