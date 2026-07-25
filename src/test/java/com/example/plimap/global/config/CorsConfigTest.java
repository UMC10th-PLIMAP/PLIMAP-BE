package com.example.plimap.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void 쿠키_인증과_CSRF_헤더를_허용한다() {
        CorsProperties properties = new CorsProperties(List.of("http://localhost:5173", "https://dev.plimap.kr"));
        CorsConfig config = new CorsConfig(properties);
        TestCorsRegistry registry = new TestCorsRegistry();

        config.addCorsMappings(registry);

        CorsConfiguration cors = registry.configurations().get("/**");
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:5173", "https://dev.plimap.kr");
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
