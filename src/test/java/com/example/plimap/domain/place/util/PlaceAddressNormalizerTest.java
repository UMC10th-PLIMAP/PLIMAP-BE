package com.example.plimap.domain.place.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaceAddressNormalizerTest {

    @Test
    void 주소의_모든_공백_문자를_제거한다() {
        String normalized = PlaceAddressNormalizer.normalize(
                " 서울특별시\t강남구\n역삼동\u00A01 "
        );

        assertThat(normalized).isEqualTo("서울특별시강남구역삼동1");
    }
}
