package com.example.plimap.global.external.itunes;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "itunes")
public record ItunesProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
