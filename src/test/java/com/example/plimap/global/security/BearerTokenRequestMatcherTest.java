package com.example.plimap.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class BearerTokenRequestMatcherTest {

    private final BearerTokenRequestMatcher matcher = new BearerTokenRequestMatcher();

    @Test
    void Bearer_토큰이_있는_요청을_식별한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");

        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void 비어_있거나_다른_인증_방식은_Bearer_요청으로_보지_않는다() {
        MockHttpServletRequest emptyBearer = new MockHttpServletRequest();
        emptyBearer.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ");
        MockHttpServletRequest basic = new MockHttpServletRequest();
        basic.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");

        assertThat(matcher.matches(new MockHttpServletRequest())).isFalse();
        assertThat(matcher.matches(emptyBearer)).isFalse();
        assertThat(matcher.matches(basic)).isFalse();
    }
}
