package com.example.plimap.global.external.storage.supabase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "profile-image.storage")
public record ProfileImageStorageProperties(
        @NotNull Provider provider,
        @NotBlank String bucket,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @PositiveOrZero long cacheControlSeconds,
        @NotNull @Valid Supabase supabase
) {

    public enum Provider {
        SUPABASE,
        GCS
    }

    public record Supabase(
            @NotNull URI url,
            @NotBlank String secretKey
    ) {
    }
}
