package com.example.plimap.global.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsPropertiesTest {

    @Test
    void Origin의_공백과_빈_값을_정리한다() {
        CorsProperties properties = new CorsProperties(List.of(
                " http://localhost:3000 ",
                "",
                "  ",
                "https://dev.plimap.com"
        ));

        assertThat(properties.allowedOrigins()).containsExactly(
                "http://localhost:3000",
                "https://dev.plimap.com"
        );
    }

    @Test
    void Origin이_없으면_교차_출처를_허용하지_않는다() {
        CorsProperties properties = new CorsProperties(null);

        assertThat(properties.allowedOrigins()).isEmpty();
    }

    @Test
    void 와일드카드_Origin은_허용하지_않는다() {
        assertThatThrownBy(() -> new CorsProperties(List.of("*")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CORS 허용 Origin에는 와일드카드(*)를 사용할 수 없습니다.");
    }
}
