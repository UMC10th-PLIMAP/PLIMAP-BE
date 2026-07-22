package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.entity.OAuthMember;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.global.security.AuthCookieUtil;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.RefreshTokenService;
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
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AuthCookieUtil authCookieUtil = new AuthCookieUtil();
    private final OAuthSuccessHandler handler = new OAuthSuccessHandler(jwtUtil, refreshTokenService, authCookieUtil);

    @Test
    void 로그인_응답에서_지연_로딩된_CSRF_토큰을_강제로_로드한다() throws Exception {
        setUpHandler();
        when(jwtUtil.createAccessToken(any())).thenReturn("access-token-value");
        when(jwtUtil.createRefreshToken(any())).thenReturn("refresh-token-value");
        when(jwtUtil.getAccessTokenExpiry()).thenReturn(Duration.ofDays(1));
        when(jwtUtil.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(14));

        MockHttpServletRequest request = new MockHttpServletRequest();
        CsrfToken csrfToken = mock(CsrfToken.class);
        request.setAttribute(CsrfToken.class.getName(), csrfToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication(true));

        verify(csrfToken).getToken();
        assertThat(response.getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith("accessToken=access-token-value"));
        assertThat(response.getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith("refreshToken=refresh-token-value"));
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/home?isNewUser=false");
    }

    @Test
    void CSRF_토큰_속성이_없어도_예외없이_로그인_응답을_내려준다() throws Exception {
        setUpHandler();
        when(jwtUtil.createAccessToken(any())).thenReturn("access-token-value");
        when(jwtUtil.createRefreshToken(any())).thenReturn("refresh-token-value");
        when(jwtUtil.getAccessTokenExpiry()).thenReturn(Duration.ofDays(1));
        when(jwtUtil.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(14));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication(true));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/home?isNewUser=false");
    }

    @Test
    void 온보딩을_완료하지_않은_유저면_리다이렉트_URL에_isNewUser_true를_붙인다() throws Exception {
        setUpHandler();
        when(jwtUtil.createAccessToken(any())).thenReturn("access-token-value");
        when(jwtUtil.createRefreshToken(any())).thenReturn("refresh-token-value");
        when(jwtUtil.getAccessTokenExpiry()).thenReturn(Duration.ofDays(1));
        when(jwtUtil.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(14));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication(false));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/home?isNewUser=true");
    }

    @Test
    void 온보딩을_완료한_유저면_리다이렉트_URL에_isNewUser_false를_붙인다() throws Exception {
        setUpHandler();
        when(jwtUtil.createAccessToken(any())).thenReturn("access-token-value");
        when(jwtUtil.createRefreshToken(any())).thenReturn("refresh-token-value");
        when(jwtUtil.getAccessTokenExpiry()).thenReturn(Duration.ofDays(1));
        when(jwtUtil.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(14));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication(true));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/home?isNewUser=false");
    }

    private void setUpHandler() {
        ReflectionTestUtils.setField(handler, "redirectUri", "http://localhost:3000/home");
        ReflectionTestUtils.setField(authCookieUtil, "cookieSecure", false);
        ReflectionTestUtils.setField(authCookieUtil, "cookieSameSite", "Lax");
    }

    private Authentication authentication(boolean isOnboarded) {
        Member member = mock(Member.class);
        when(member.isOnboarded()).thenReturn(isOnboarded);
        OAuthMember oAuthMember = new OAuthMember(member, Collections.emptyMap());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuthMember);
        return authentication;
    }
}
