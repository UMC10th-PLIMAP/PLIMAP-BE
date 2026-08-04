package com.example.plimap.global.external.storage.gcs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "profile-image.storage.gcs")
public record GcsProfileImageStorageProperties(
        @NotBlank(message = "GCS 버킷 이름은 필수입니다.")
        String bucket,
        @NotNull(message = "GCS 공개 URL 기준 주소는 필수입니다.")
        URI publicBaseUrl,
        @PositiveOrZero(message = "프로필 이미지 캐시 시간은 0 이상이어야 합니다.")
        long cacheControlSeconds
) {
}