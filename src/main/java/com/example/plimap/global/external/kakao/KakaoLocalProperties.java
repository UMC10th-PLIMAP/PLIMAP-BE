package com.example.plimap.global.external.kakao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kakao.local")
@Validated
public record KakaoLocalProperties(
        @NotNull
        URI baseUrl,
        @NotBlank
        String restApiKey,
        @NotNull
        Duration connectTimeout,
        @NotNull
        Duration readTimeout
) {
}
