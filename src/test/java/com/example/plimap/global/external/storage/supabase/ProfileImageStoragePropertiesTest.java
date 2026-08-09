package com.example.plimap.global.external.storage.supabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProfileImageStoragePropertiesTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 타임아웃이_0이거나_음수면_검증에_실패한다() {
        // given
        ProfileImageStorageProperties properties = properties(
                Duration.ZERO,
                Duration.ofSeconds(-1)
        );

        // when
        Set<ConstraintViolation<ProfileImageStorageProperties>> violations =
                validator.validate(properties);

        // then
        assertThat(violations)
                .extracting(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                "connectTimeout",
                                "스토리지 연결 제한 시간은 0보다 커야 합니다."
                        ),
                        tuple(
                                "readTimeout",
                                "스토리지 응답 제한 시간은 0보다 커야 합니다."
                        )
                );
    }

    @Test
    void 타임아웃이_null이면_한글_필수_메시지로_검증에_실패한다() {
        // given
        ProfileImageStorageProperties properties = properties(null, null);

        // when
        Set<ConstraintViolation<ProfileImageStorageProperties>> violations =
                validator.validate(properties);

        // then
        assertThat(violations)
                .extracting(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                "connectTimeout",
                                "스토리지 연결 제한 시간은 필수입니다."
                        ),
                        tuple(
                                "readTimeout",
                                "스토리지 응답 제한 시간은 필수입니다."
                        )
                );
    }

    @Test
    void 양수_타임아웃이면_검증에_성공한다() {
        // given
        ProfileImageStorageProperties properties = properties(
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)
        );

        // when
        Set<ConstraintViolation<ProfileImageStorageProperties>> violations =
                validator.validate(properties);

        // then
        assertThat(violations).isEmpty();
    }

    private ProfileImageStorageProperties properties(
            Duration connectTimeout,
            Duration readTimeout
    ) {
        return new ProfileImageStorageProperties(
                ProfileImageStorageProperties.Provider.SUPABASE,
                "profile-images",
                connectTimeout,
                readTimeout,
                31_536_000,
                new ProfileImageStorageProperties.Supabase(
                        URI.create("https://project.supabase.co"),
                        "sb_secret_test-value"
                )
        );
    }
}
