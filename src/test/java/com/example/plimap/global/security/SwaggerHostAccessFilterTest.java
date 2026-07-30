package com.example.plimap.global.security;

import com.example.plimap.global.config.SwaggerAccessProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SwaggerHostAccessFilterTest {

    private final SwaggerHostAccessFilter filter = new SwaggerHostAccessFilter(
            new SwaggerAccessProperties("https://dev.plimap.kr")
    );

    @Test
    void 공식_Dev_Host의_Swagger_요청은_허용한다() throws Exception {
        MockHttpServletRequest request = request(
                "dev.plimap.kr",
                "/swagger-ui/index.html"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 공식_Dev_Host의_OpenAPI_하위_경로도_허용한다() throws Exception {
        MockHttpServletRequest request = request(
                "dev.plimap.kr",
                "/v3/api-docs/swagger-config"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void Cloud_Run_기본_Host의_Swagger_요청은_404로_숨긴다() throws Exception {
        MockHttpServletRequest request = request(
                "plimap-api-dev-example.run.app",
                "/swagger-ui/index.html"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(404);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 비공식_Host라도_일반_API_요청은_제한하지_않는다() throws Exception {
        MockHttpServletRequest request = request(
                "plimap-api-dev-example.run.app",
                "/api/v1/auth/csrf"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest request(String serverName, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setServerName(serverName);
        return request;
    }
}
