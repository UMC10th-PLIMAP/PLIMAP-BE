package com.example.plimap.global.config;

import com.example.plimap.domain.member.service.command.MemberCommandService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CorsConfigTest {

    @Test
    void 쿠키_인증과_CSRF_헤더를_허용한다() {
        CorsProperties properties = new CorsProperties(List.of(
                "http://localhost:5173",
                "https://dev.plimap.kr",
                "https://pr-*.plimap.kr",
                "http://192.168.*:[*]",
                "https://192.168.*:[*]"
        ));
        CorsConfig config = new CorsConfig(properties, mock(MemberCommandService.class));
        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns()).containsExactly(
                "http://localhost:5173",
                "https://dev.plimap.kr",
                "https://pr-*.plimap.kr",
                "http://192.168.*:[*]",
                "https://192.168.*:[*]"
        );
        assertThat(cors.checkOrigin("https://pr-123.plimap.kr")).isEqualTo("https://pr-123.plimap.kr");
        assertThat(cors.checkOrigin("http://192.168.1.10:5173")).isEqualTo("http://192.168.1.10:5173");
        assertThat(cors.checkOrigin("https://192.168.10.20:8080")).isEqualTo("https://192.168.10.20:8080");
        assertThat(cors.checkOrigin("http://192.168.256.10:5173")).isNull();
        assertThat(cors.checkOrigin("http://192.168.1.10.evil:5173")).isNull();
        assertThat(cors.checkOrigin("https://evil.plimap.kr")).isNull();
        assertThat(cors.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders())
                .containsExactly("Authorization", "Content-Type", "X-XSRF-TOKEN");
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}
