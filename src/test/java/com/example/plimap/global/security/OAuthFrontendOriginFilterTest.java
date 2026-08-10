package com.example.plimap.global.security;

import com.example.plimap.global.config.OAuthProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthFrontendOriginFilterTest {

    private final AuthCookieUtil authCookieUtil = new AuthCookieUtil();
    private final OAuthProperties oAuthProperties = new OAuthProperties(
            "https://dev.plimap.kr/home",
            List.of("https://dev.plimap.kr", "http://localhost:5173", "https://pr-*.plimap.kr")
    );
    private final OAuthFrontendRedirectCookieRepository repository =
            new OAuthFrontendRedirectCookieRepository(authCookieUtil, oAuthProperties);
    private final OAuthFrontendOriginFilter filter = new OAuthFrontendOriginFilter(repository);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authCookieUtil, "cookieSecure", true);
        ReflectionTestUtils.setField(authCookieUtil, "cookieSameSite", "None");
    }

    @Test
    void 허용된_Origin이면_OAuth_인가_요청을_계속_처리한다() throws Exception {
        MockHttpServletRequest request = authorizationRequest("http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(response.getCookie(OAuthFrontendRedirectCookieRepository.COOKIE_NAME)).isNull();
    }

    @Test
    void 허용되지_않은_Origin이면_400으로_거부한다() throws Exception {
        MockHttpServletRequest request = authorizationRequest("https://attacker.example");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void OAuth_인가_경로가_아니면_Origin_쿠키를_생성하지_않는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/terms");
        request.setServletPath("/api/v1/auth/terms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(response.getCookie(OAuthFrontendRedirectCookieRepository.COOKIE_NAME)).isNull();
    }

    @Test
    void OAuth_인가_경로에_하위_경로가_추가되면_검증하지_않는다() throws Exception {
        MockHttpServletRequest request = authorizationRequest(
                "GET",
                "/oauth/authorization/google/extra",
                "",
                "https://attacker.example"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void OAuth_인가_기본_경로만_요청하면_검증하지_않는다() throws Exception {
        MockHttpServletRequest request = authorizationRequest(
                "GET",
                "/oauth/authorization/",
                "",
                "https://attacker.example"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void GET이_아니면_OAuth_인가_경로여도_검증하지_않는다() throws Exception {
        MockHttpServletRequest request = authorizationRequest(
                "POST",
                "/oauth/authorization/google",
                "",
                "https://attacker.example"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void contextPath가_있어도_OAuth_인가_경로를_검증한다() throws Exception {
        MockHttpServletRequest request = authorizationRequest(
                "GET",
                "/app/oauth/authorization/google",
                "/app",
                "https://attacker.example"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void 허용된_Preview_Origin이면_OAuth_인가_요청을_계속_처리한다() throws Exception {
        MockHttpServletRequest request = authorizationRequest("https://pr-123.plimap.kr");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest authorizationRequest(String frontendOrigin) {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/oauth/authorization/google");
        request.setServletPath("/oauth/authorization/google");
        request.addParameter(OAuthFrontendRedirectCookieRepository.PARAMETER_NAME, frontendOrigin);
        return request;
    }

    private MockHttpServletRequest authorizationRequest(String method,
                                                        String requestUri,
                                                        String contextPath,
                                                        String frontendOrigin) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        request.setContextPath(contextPath);
        request.setServletPath(requestUri.substring(contextPath.length()));
        request.addParameter(OAuthFrontendRedirectCookieRepository.PARAMETER_NAME, frontendOrigin);
        return request;
    }
}
