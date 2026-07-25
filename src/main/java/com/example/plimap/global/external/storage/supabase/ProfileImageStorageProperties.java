package com.example.plimap.global.external.storage.supabase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "profile-image.storage")
public record ProfileImageStorageProperties(
        @NotNull Provider provider,
        @NotBlank String bucket,
        @NotNull(message = "스토리지 연결 제한 시간은 필수입니다.")
        @DurationMin(
                nanos = 0,
                inclusive = false,
                message = "스토리지 연결 제한 시간은 0보다 커야 합니다."
        )
        Duration connectTimeout,
        @NotNull(message = "스토리지 응답 제한 시간은 필수입니다.")
        @DurationMin(
                nanos = 0,
                inclusive = false,
                message = "스토리지 응답 제한 시간은 0보다 커야 합니다."
        )
        Duration readTimeout,
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
