package com.example.plimap.global.external.storage.gcs;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GcsProfileImageStoragePropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 올바른_GCS_설정은_검증을_통과한다() {
        GcsProfileImageStorageProperties properties = new GcsProfileImageStorageProperties(
                "plimap-prod-profile-images",
                URI.create("https://storage.googleapis.com"),
                31536000
        );

        Set<ConstraintViolation<GcsProfileImageStorageProperties>> violations =
                validator.validate(properties);

        assertThat(violations).isEmpty();
    }

    @Test
    void 버킷_이름이_비어있으면_검증에_실패한다() {
        GcsProfileImageStorageProperties properties = new GcsProfileImageStorageProperties(
                " ",
                URI.create("https://storage.googleapis.com"),
                31536000
        );

        Set<ConstraintViolation<GcsProfileImageStorageProperties>> violations =
                validator.validate(properties);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("GCS 버킷 이름은 필수입니다.");
    }

    @Test
    void 캐시_시간이_음수이면_검증에_실패한다() {
        GcsProfileImageStorageProperties properties = new GcsProfileImageStorageProperties(
                "plimap-prod-profile-images",
                URI.create("https://storage.googleapis.com"),
                -1
        );

        Set<ConstraintViolation<GcsProfileImageStorageProperties>> violations =
                validator.validate(properties);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("프로필 이미지 캐시 시간은 0 이상이어야 합니다.");
    }
}