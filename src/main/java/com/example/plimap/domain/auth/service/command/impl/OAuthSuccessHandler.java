package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.auth.entity.OAuthMember;
import com.example.plimap.global.security.AuthCookieUtil;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieUtil authCookieUtil;

    @Value("${oauth.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuthMember oAuthMember = (OAuthMember) authentication.getPrincipal();
        AuthMember authMember = new AuthMember(oAuthMember.getMember());
        String accessToken = jwtUtil.createAccessToken(authMember);
        String refreshToken = jwtUtil.createRefreshToken(authMember);

        refreshTokenService.save(oAuthMember.getMember().getId(), refreshToken, jwtUtil.getRefreshTokenExpiry());
        authCookieUtil.setCookie(response, "accessToken", accessToken, jwtUtil.getAccessTokenExpiry());
        authCookieUtil.setCookie(response, "refreshToken", refreshToken, jwtUtil.getRefreshTokenExpiry());

        // CsrfFilter가 지연 로딩해둔 CsrfToken을 여기서 강제로 로드해야
        // CsrfCookieFilter(로그인 이후 필터)에 도달하기 전에 리다이렉트로 응답이 끝나도
        // 로그인 응답에 XSRF-TOKEN 쿠키가 함께 내려간다.
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        response.sendRedirect(redirectUri);
    }
}
