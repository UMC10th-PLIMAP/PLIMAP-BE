package com.example.plimap.global.config;

import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {

    @Test
    void 테스트_토큰_발급_키를_API_Key_헤더로_등록한다() {
        SecurityScheme issueKeyScheme = new SwaggerConfig()
                .swagger()
                .getComponents()
                .getSecuritySchemes()
                .get(SwaggerConfig.TEST_TOKEN_ISSUE_KEY_SCHEME);

        assertThat(issueKeyScheme.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
        assertThat(issueKeyScheme.getIn()).isEqualTo(SecurityScheme.In.HEADER);
        assertThat(issueKeyScheme.getName()).isEqualTo(SwaggerConfig.TEST_TOKEN_ISSUE_KEY_HEADER);
    }
}
