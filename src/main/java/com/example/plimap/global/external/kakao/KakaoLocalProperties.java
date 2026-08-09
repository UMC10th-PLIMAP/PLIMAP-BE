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
        @NotNull(message = "Kakao Local API 기본 URL은 필수입니다.")
        URI baseUrl,
        @NotBlank(message = "Kakao Local API REST API 키는 필수입니다.")
        String restApiKey,
        @NotNull(message = "Kakao Local API 연결 제한 시간은 필수입니다.")
        Duration connectTimeout,
        @NotNull(message = "Kakao Local API 응답 제한 시간은 필수입니다.")
        Duration readTimeout
) {
}
