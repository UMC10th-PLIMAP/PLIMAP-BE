package com.example.plimap.global.security;

import com.example.plimap.global.config.SwaggerAccessProperties;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerForwardedHostIntegrationTest {

    @Test
    void forwarded_Host가_공식_Dev_Host이면_Swagger를_허용한다() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        request.setServerName("plimap-api-dev-example.run.app");
        request.addHeader("X-Forwarded-Host", "dev.plimap.kr");
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedSwagger = new AtomicBoolean(false);

        SwaggerHostAccessFilter swaggerFilter = new SwaggerHostAccessFilter(
                new SwaggerAccessProperties("https://dev.plimap.kr")
        );
        FilterChain swaggerChain = (forwardedRequest, forwardedResponse) ->
                swaggerFilter.doFilter(
                        forwardedRequest,
                        forwardedResponse,
                        (ignoredRequest, ignoredResponse) -> reachedSwagger.set(true)
                );

        new ForwardedHeaderFilter().doFilter(request, response, swaggerChain);

        assertThat(reachedSwagger).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
