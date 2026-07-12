package com.example.plimap.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plimap.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList();

        if (allowedOrigins.contains("*")) {
            throw new IllegalArgumentException("CORS 허용 Origin에는 와일드카드(*)를 사용할 수 없습니다.");
        }
    }
}
