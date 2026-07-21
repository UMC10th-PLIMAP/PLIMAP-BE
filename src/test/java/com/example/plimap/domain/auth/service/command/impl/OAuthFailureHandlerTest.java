package com.example.plimap.domain.auth.service.command.impl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthFailureHandlerTest {

    private final OAuthFailureHandler handler = new OAuthFailureHandler();

    @Test
    void 인증_실패시_에러_파라미터와_함께_리다이렉트한다() throws Exception {
        ReflectionTestUtils.setField(handler, "redirectUri", "http://localhost:3000/home");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception =
                new OAuth2AuthenticationException(new OAuth2Error("invalid_grant"), "invalid_grant");

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/home?error=oauth_login_failed");
    }
}
