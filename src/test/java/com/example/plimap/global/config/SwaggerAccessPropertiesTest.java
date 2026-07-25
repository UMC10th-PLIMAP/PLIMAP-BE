package com.example.plimap.global.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SwaggerAccessPropertiesTest {

    @Test
    void HTTPS_Origin에서_Swagger_허용_Host를_추출한다() {
        SwaggerAccessProperties properties =
                new SwaggerAccessProperties("https://DEV.PLIMAP.KR/");

        assertThat(properties.publicOrigin()).isEqualTo("https://dev.plimap.kr");
        assertThat(properties.allowedHost()).isEqualTo("dev.plimap.kr");
    }

    @Test
    void Swagger_공개_Origin이_비어_있으면_거부한다() {
        assertThatThrownBy(() -> new SwaggerAccessProperties(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Swagger 공개 Origin은 비어 있을 수 없습니다.");
    }

    @Test
    void HTTPS가_아닌_Origin은_거부한다() {
        assertThatThrownBy(() -> new SwaggerAccessProperties("http://dev.plimap.kr"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 경로나_query가_있는_Origin은_거부한다() {
        assertThatThrownBy(() -> new SwaggerAccessProperties("https://dev.plimap.kr/swagger"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SwaggerAccessProperties("https://dev.plimap.kr?from=test"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
