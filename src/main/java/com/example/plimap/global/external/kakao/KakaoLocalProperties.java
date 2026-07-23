package com.example.plimap.global.external.kakao;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.local")
public record KakaoLocalProperties(
        URI baseUrl,
        String restApiKey,
        Duration connectTimeout,
        Duration readTimeout
) {
}
