package com.example.plimap.global.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private static final String COOKIE_NAME = "oauth2_auth_request";

    private final AuthCookieUtil authCookieUtil = new AuthCookieUtil();
    private final HttpCookieOAuth2AuthorizationRequestRepository repository =
            new HttpCookieOAuth2AuthorizationRequestRepository(authCookieUtil, new ObjectMapper());

    @Test
    void 저장한_인가_요청을_쿠키에서_그대로_조회한다() {
        setUpAuthCookieUtil();
        MockHttpServletRequest saveRequest = new MockHttpServletRequest();
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authorizationRequest(), saveRequest, saveResponse);
        MockHttpServletRequest loadRequest = requestWithSavedCookie(saveResponse);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getClientId()).isEqualTo("test-client-id");
        assertThat(loaded.getState()).isEqualTo("test-state");
        assertThat(loaded.getAuthorizationRequestUri())
                .startsWith("https://kauth.kakao.com/oauth/authorize");
    }

    @Test
    void 쿠키가_없으면_null을_반환한다() {
        setUpAuthCookieUtil();
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(repository.loadAuthorizationRequest(request)).isNull();
    }

    @Test
    void 인가_요청이_null이면_쿠키를_즉시_만료시킨다() {
        setUpAuthCookieUtil();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(null, request, response);

        assertThat(response.getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith(COOKIE_NAME + "=") && header.contains("Max-Age=0"));
    }

    @Test
    void 인가_요청_삭제시_저장된_값을_반환하고_쿠키를_만료시킨다() {
        setUpAuthCookieUtil();
        MockHttpServletRequest saveRequest = new MockHttpServletRequest();
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorizationRequest(), saveRequest, saveResponse);
        MockHttpServletRequest removeRequest = requestWithSavedCookie(saveResponse);
        MockHttpServletResponse removeResponse = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed =
                repository.removeAuthorizationRequest(removeRequest, removeResponse);

        assertThat(removed).isNotNull();
        assertThat(removed.getState()).isEqualTo("test-state");
        assertThat(removeResponse.getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith(COOKIE_NAME + "=") && header.contains("Max-Age=0"));
    }

    private void setUpAuthCookieUtil() {
        ReflectionTestUtils.setField(authCookieUtil, "cookieSecure", false);
        ReflectionTestUtils.setField(authCookieUtil, "cookieSameSite", "Lax");
    }

    private MockHttpServletRequest requestWithSavedCookie(MockHttpServletResponse savedResponse) {
        String setCookieHeader = savedResponse.getHeaders("Set-Cookie").stream()
                .filter(header -> header.startsWith(COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();
        String cookieValue = setCookieHeader.substring(
                (COOKIE_NAME + "=").length(), setCookieHeader.indexOf(';'));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, cookieValue));
        return request;
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client-id")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .redirectUri("https://plimap-api-dev.example.com/oauth/callback/kakao")
                .state("test-state")
                .build();
    }
}
