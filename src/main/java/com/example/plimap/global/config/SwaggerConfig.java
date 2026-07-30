package com.example.plimap.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String TEST_TOKEN_ISSUE_KEY_SCHEME = "DEV TEST TOKEN KEY";
    public static final String TEST_TOKEN_ISSUE_KEY_HEADER = "X-Dev-Token-Key";

    @Bean
    public OpenAPI swagger() {
        Info info = new Info()
                .title("PLIMAP API")
                .description("PLIMAP 백엔드 API 명세")
                .version("0.0.1");

        // JWT 토큰 헤더 방식
        String securityScheme = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityScheme);
        Components components = new Components()
                .addSecuritySchemes(securityScheme, new SecurityScheme()
                        .name(securityScheme)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes(TEST_TOKEN_ISSUE_KEY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(TEST_TOKEN_ISSUE_KEY_HEADER));

        return new OpenAPI()
                .info(info)
                .addServersItem(new Server().url("/"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
