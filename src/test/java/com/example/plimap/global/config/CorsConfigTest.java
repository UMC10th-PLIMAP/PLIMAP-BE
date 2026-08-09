package com.example.plimap.global.config;

import com.example.plimap.domain.member.service.command.MemberCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CorsConfigTest {

    @Test
    void 쿠키_인증과_CSRF_헤더를_허용한다() {
        CorsProperties properties = new CorsProperties(List.of(
                "http://localhost:5173",
                "https://dev.plimap.kr",
                "https://pr-*.plimap.kr"
        ));
        CorsConfig config = new CorsConfig(properties, mock(MemberCommandService.class));
        TestCorsRegistry registry = new TestCorsRegistry();

        config.addCorsMappings(registry);

        CorsConfiguration cors = registry.configurations().get("/**");
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns()).containsExactly(
                "http://localhost:5173",
                "https://dev.plimap.kr",
                "https://pr-*.plimap.kr"
        );
        assertThat(cors.checkOrigin("https://pr-123.plimap.kr")).isEqualTo("https://pr-123.plimap.kr");
        assertThat(cors.checkOrigin("https://evil.plimap.kr")).isNull();
        assertThat(cors.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders())
                .containsExactly("Authorization", "Content-Type", "X-XSRF-TOKEN");
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    private static final class TestCorsRegistry extends CorsRegistry {

        Map<String, CorsConfiguration> configurations() {
            return super.getCorsConfigurations();
        }
    }
}
