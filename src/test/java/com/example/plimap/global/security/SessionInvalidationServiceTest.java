package com.example.plimap.global.security;

import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionInvalidationServiceTest {

    private static final String TOKEN = "valid-token";
    private static final String JTI = "jti-1";
    private static final Long MEMBER_ID = 1L;

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AuthCookieUtil authCookieUtil = mock(AuthCookieUtil.class);

    private final SessionInvalidationService sessionInvalidationService =
            new SessionInvalidationService(jwtUtil, tokenBlacklistService, refreshTokenService, authCookieUtil);

    @Test
    void 유효한_토큰이면_블랙리스트_등록과_리프레시_토큰_삭제_및_쿠키_제거를_수행한다() {
        when(jwtUtil.isValid(TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(TOKEN)).thenReturn(JTI);
        when(jwtUtil.getRemainingExpiry(TOKEN)).thenReturn(Duration.ofHours(1));
        when(jwtUtil.getMemberId(TOKEN)).thenReturn(MEMBER_ID);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        sessionInvalidationService.invalidate(request, response);

        verify(tokenBlacklistService).blacklist(JTI, Duration.ofHours(1));
        verify(refreshTokenService).delete(MEMBER_ID);
        verify(authCookieUtil).clearCookie(response, "accessToken");
        verify(authCookieUtil).clearCookie(response, "refreshToken");
    }

    @Test
    void 토큰이_없으면_블랙리스트_등록과_리프레시_토큰_삭제_없이_쿠키만_제거한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        sessionInvalidationService.invalidate(request, response);

        verify(tokenBlacklistService, never()).blacklist(any(), any());
        verify(refreshTokenService, never()).delete(any());
        verify(authCookieUtil).clearCookie(response, "accessToken");
        verify(authCookieUtil).clearCookie(response, "refreshToken");
    }

    @Test
    void 유효하지_않은_토큰이면_블랙리스트_등록과_리프레시_토큰_삭제_없이_쿠키만_제거한다() {
        when(jwtUtil.isValid(TOKEN)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        sessionInvalidationService.invalidate(request, response);

        verify(tokenBlacklistService, never()).blacklist(any(), any());
        verify(refreshTokenService, never()).delete(any());
        verify(authCookieUtil).clearCookie(response, "accessToken");
        verify(authCookieUtil).clearCookie(response, "refreshToken");
    }
}
