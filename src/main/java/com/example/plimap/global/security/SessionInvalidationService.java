package com.example.plimap.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionInvalidationService {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieUtil authCookieUtil;

    public void invalidate(HttpServletRequest request, HttpServletResponse response) {
        String token = TokenResolver.resolve(request);
        if (token != null && jwtUtil.isValid(token)) {
            tokenBlacklistService.blacklist(jwtUtil.getJti(token), jwtUtil.getRemainingExpiry(token));
            refreshTokenService.delete(jwtUtil.getMemberId(token));
        }

        authCookieUtil.clearCookie(response, "accessToken");
        authCookieUtil.clearCookie(response, "refreshToken");
    }
}
