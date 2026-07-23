package com.example.plimap.global.external.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProfileImageObjectKeyGeneratorTest {

    private final ProfileImageObjectKeyGenerator generator =
            new ProfileImageObjectKeyGenerator();

    @Test
    void 회원_ID와_UUID로_새로운_WebP_객체_키를_생성한다() {
        String firstKey = generator.generate(42L);
        String secondKey = generator.generate(42L);

        assertThat(firstKey).matches(
                "members/42/[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}"
                        + "-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.webp"
        );
        assertThat(secondKey).isNotEqualTo(firstKey);
    }

    @Test
    void 유효하지_않은_회원_ID로는_객체_키를_생성하지_않는다() {
        assertThatThrownBy(() -> generator.generate(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
