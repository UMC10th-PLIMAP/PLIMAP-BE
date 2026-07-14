package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.entity.OAuthMember;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.global.security.JwtUtil;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthSuccessHandlerTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final OAuthSuccessHandler handler = new OAuthSuccessHandler(jwtUtil);

    @Test
    void 로그인_응답에서_지연_로딩된_CSRF_토큰을_강제로_로드한다() throws Exception {
        setUpHandler();
        when(jwtUtil.createAccessToken(any())).thenReturn("access-token-value");
        when(jwtUtil.getAccessTokenExpiry()).thenReturn(Duration.ofDays(1));

        MockHttpServletRequest request = new MockHttpServletRequest();
        CsrfToken csrfToken = mock(CsrfToken.class);
        request.setAttribute(CsrfToken.class.getName(), csrfToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication());

        verify(csrfToken).getToken();
        assertThat(response.getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith("accessToken=access-token-value"));
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/home");
    }

    @Test
    void CSRF_토큰_속성이_없어도_예외없이_로그인_응답을_내려준다() throws Exception {
        setUpHandler();
        when(jwtUtil.createAccessToken(any())).thenReturn("access-token-value");
        when(jwtUtil.getAccessTokenExpiry()).thenReturn(Duration.ofDays(1));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication());

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/home");
    }

    private void setUpHandler() {
        ReflectionTestUtils.setField(handler, "redirectUri", "http://localhost:3000/home");
        ReflectionTestUtils.setField(handler, "cookieSecure", false);
        ReflectionTestUtils.setField(handler, "cookieSameSite", "Lax");
    }

    private Authentication authentication() {
        Member member = mock(Member.class);
        OAuthMember oAuthMember = new OAuthMember(member, Collections.emptyMap());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuthMember);
        return authentication;
    }
}
