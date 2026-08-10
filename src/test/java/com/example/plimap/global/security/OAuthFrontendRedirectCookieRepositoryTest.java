package com.example.plimap.global.security;

import com.example.plimap.global.config.OAuthProperties;
import jakarta.servlet.http.Cookie;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthFrontendRedirectCookieRepositoryTest {

    private final AuthCookieUtil authCookieUtil = new AuthCookieUtil();
    private final OAuthProperties oAuthProperties = new OAuthProperties(
            "https://dev.plimap.kr/home",
            List.of("https://dev.plimap.kr", "http://localhost:5173", "https://pr-*.plimap.kr")
    );
    private final OAuthFrontendRedirectCookieRepository repository =
            new OAuthFrontendRedirectCookieRepository(authCookieUtil, oAuthProperties);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authCookieUtil, "cookieSecure", true);
        ReflectionTestUtils.setField(authCookieUtil, "cookieSameSite", "None");
    }

    @Test
    void 허용된_로컬_Origin을_짧은_수명의_HttpOnly_쿠키에_저장한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(OAuthFrontendRedirectCookieRepository.PARAMETER_NAME, "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveRequestedOrigin(request, response, "test-state");

        Cookie cookie = response.getCookie(OAuthFrontendRedirectCookieRepository.COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("None");
        assertThat(cookie.getMaxAge()).isEqualTo(180);
    }

    @Test
    void Origin을_생략하면_기본_Dev_Origin을_저장한다() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveRequestedOrigin(new MockHttpServletRequest(), response, "test-state");

        MockHttpServletRequest callbackRequest = requestWithCookie(response);
        callbackRequest.addParameter("state", "test-state");
        MockHttpServletResponse callbackResponse = new MockHttpServletResponse();

        assertThat(repository.consumeRedirectUri(callbackRequest, callbackResponse))
                .isEqualTo("https://dev.plimap.kr/home");
    }

    @Test
    void 저장된_로컬_Origin을_로그인_완료_URI로_소비하고_쿠키를_삭제한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(OAuthFrontendRedirectCookieRepository.PARAMETER_NAME, "http://localhost:5173");
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repository.saveRequestedOrigin(request, saveResponse, "test-state");
        MockHttpServletRequest callbackRequest = requestWithCookie(saveResponse);
        callbackRequest.addParameter("state", "test-state");
        MockHttpServletResponse callbackResponse = new MockHttpServletResponse();

        String redirectUri = repository.consumeRedirectUri(callbackRequest, callbackResponse);

        assertThat(redirectUri).isEqualTo("http://localhost:5173/home");
        assertThat(callbackResponse.getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith(OAuthFrontendRedirectCookieRepository.COOKIE_NAME + "=")
                        && header.contains("Max-Age=0"));
    }

    @Test
    void 허용되지_않은_Origin은_저장하지_않는다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(OAuthFrontendRedirectCookieRepository.PARAMETER_NAME, "https://attacker.example");

        assertThatThrownBy(() -> repository.saveRequestedOrigin(request, new MockHttpServletResponse(), "test-state"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않은 OAuth 프론트 Origin입니다.");
    }

    @Test
    void 변조된_쿠키는_기본_Dev_리다이렉트로_대체한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String attackerOrigin = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("test-state\nhttps://attacker.example".getBytes());
        request.setCookies(new Cookie(OAuthFrontendRedirectCookieRepository.COOKIE_NAME, attackerOrigin));
        request.addParameter("state", "test-state");

        assertThat(repository.consumeRedirectUri(request, new MockHttpServletResponse()))
                .isEqualTo("https://dev.plimap.kr/home");
    }

    @Test
    void 저장된_state와_콜백_state가_다르면_기본_Dev_리다이렉트로_대체한다() {
        MockHttpServletRequest authorizationRequest = new MockHttpServletRequest();
        authorizationRequest.addParameter(
                OAuthFrontendRedirectCookieRepository.PARAMETER_NAME,
                "http://localhost:5173"
        );
        MockHttpServletResponse authorizationResponse = new MockHttpServletResponse();
        repository.saveRequestedOrigin(authorizationRequest, authorizationResponse, "saved-state");

        MockHttpServletRequest callbackRequest = requestWithCookie(authorizationResponse);
        callbackRequest.addParameter("state", "different-state");

        assertThat(repository.consumeRedirectUri(
                callbackRequest,
                new MockHttpServletResponse()
        )).isEqualTo("https://dev.plimap.kr/home");
    }

    @Test
    void Preview_Origin을_로그인_완료_URI로_변환한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(
                OAuthFrontendRedirectCookieRepository.PARAMETER_NAME,
                "https://pr-123.plimap.kr"
        );
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repository.saveRequestedOrigin(request, saveResponse, "test-state");

        MockHttpServletRequest callbackRequest = requestWithCookie(saveResponse);
        callbackRequest.addParameter("state", "test-state");

        assertThat(repository.consumeRedirectUri(
                callbackRequest,
                new MockHttpServletResponse()
        )).isEqualTo("https://pr-123.plimap.kr/home");
    }

    private MockHttpServletRequest requestWithCookie(MockHttpServletResponse response) {
        Cookie cookie = response.getCookie(OAuthFrontendRedirectCookieRepository.COOKIE_NAME);
        assertThat(cookie).isNotNull();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(cookie);
        return request;
    }
}
