package com.example.plimap.global.external.youtube;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "youtube.api")
public record YoutubeProperties(
        URI baseUrl,
        @NotBlank String key,
        Duration connectTimeout,
        Duration readTimeout
) {
}
