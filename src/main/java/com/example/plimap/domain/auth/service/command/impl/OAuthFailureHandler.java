package com.example.plimap.domain.auth.service.command.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@Slf4j
public class OAuthFailureHandler implements AuthenticationFailureHandler {

    @Value("${oauth.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 기본 SimpleUrlAuthenticationFailureHandler는 로그 없이 /login?error로만
        // 리다이렉트해서 실패 원인이 어디에도 남지 않는다. 여기서 명시적으로 기록한다.
        log.warn("OAuth2 로그인 실패: method={} uri={}", request.getMethod(), request.getRequestURI(), exception);

        String redirectLocation = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", "oauth_login_failed")
                .build()
                .toUriString();
        response.sendRedirect(redirectLocation);
    }
}
