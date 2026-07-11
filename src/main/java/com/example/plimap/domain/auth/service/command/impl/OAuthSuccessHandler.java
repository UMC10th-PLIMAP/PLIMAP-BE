package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.auth.entity.OAuthMember;
import com.example.plimap.global.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Value("${oauth.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuthMember oAuthMember = (OAuthMember) authentication.getPrincipal();
        String accessToken = jwtUtil.createAccessToken(new AuthMember(oAuthMember.getMember()));

        String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUri + "?token=" + encodedToken);
    }
}
