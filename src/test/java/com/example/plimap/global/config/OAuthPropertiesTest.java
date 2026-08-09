package com.example.plimap.global.config;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthPropertiesTest {

    @Test
    void 기본_리다이렉트_Origin만_설정하면_자동으로_허용한다() {
        OAuthProperties properties = new OAuthProperties(
                "http://localhost:5173/home",
                null
        );

        assertThat(properties.allowedFrontendOrigins())
                .containsExactly("http://localhost:5173");
        assertThat(properties.defaultFrontendOrigin())
                .isEqualTo("http://localhost:5173");
    }

    @Test
    void 허용된_Origin에_기본_리다이렉트_경로와_query를_결합한다() {
        OAuthProperties properties = new OAuthProperties(
                "https://dev.plimap.kr/home?foo=bar",
                List.of("https://dev.plimap.kr", "http://localhost:5173")
        );

        assertThat(properties.redirectUriFor("http://localhost:5173"))
                .isEqualTo("http://localhost:5173/home?foo=bar");
    }

    @Test
    void 허용되지_않은_Origin은_거부한다() {
        OAuthProperties properties = new OAuthProperties(
                "https://dev.plimap.kr/home",
                List.of("https://dev.plimap.kr", "http://localhost:5173")
        );

        assertThatThrownBy(() -> properties.requireAllowedFrontendOrigin("https://attacker.example"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않은 OAuth 프론트 Origin입니다.");
    }

    @Test
    void 경로나_query가_포함된_Origin은_거부한다() {
        assertThatThrownBy(() -> new OAuthProperties(
                "https://dev.plimap.kr/home",
                List.of("https://dev.plimap.kr/path")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new OAuthProperties(
                "https://dev.plimap.kr/home",
                List.of("https://dev.plimap.kr?next=home")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void localhost가_아닌_HTTP_Origin은_거부한다() {
        assertThatThrownBy(() -> new OAuthProperties(
                "https://dev.plimap.kr/home",
                List.of("https://dev.plimap.kr", "http://example.com")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("HTTP OAuth 프론트 Origin은 localhost에서만 허용됩니다.");
    }

    @Test
    void 기본_리다이렉트_Origin이_allowlist에_없으면_설정을_거부한다() {
        assertThatThrownBy(() -> new OAuthProperties(
                "https://dev.plimap.kr/home",
                List.of("http://localhost:5173")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAuth 기본 리다이렉트 Origin은 허용된 프론트 Origin에 포함되어야 합니다.");
    }
}
