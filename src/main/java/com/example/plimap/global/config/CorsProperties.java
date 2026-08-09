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

        if (allowedOrigins.contains("*")
                || allowedOrigins.stream()
                        .anyMatch(origin -> origin.contains("*")
                                && !origin.equals("https://pr-*.plimap.kr"))) {
            throw new IllegalArgumentException("CORS 허용 Origin에는 https://pr-*.plimap.kr 패턴 외 와일드카드를 사용할 수 없습니다.");
        }
    }
}
