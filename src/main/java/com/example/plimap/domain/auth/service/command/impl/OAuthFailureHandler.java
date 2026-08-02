package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.exception.WithdrawnMemberAuthenticationException;
import com.example.plimap.global.security.OAuthFrontendRedirectCookieRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuthFailureHandler implements AuthenticationFailureHandler {

    private final OAuthFrontendRedirectCookieRepository redirectCookieRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 기본 SimpleUrlAuthenticationFailureHandler는 로그 없이 /login?error로만
        // 리다이렉트해서 실패 원인이 어디에도 남지 않는다. 여기서 명시적으로 기록한다.
        log.warn("OAuth2 로그인 실패: method={} uri={}", request.getMethod(), request.getRequestURI(), exception);

        String errorCode = exception instanceof WithdrawnMemberAuthenticationException
                ? "account_permanently_banned"
                : "oauth_login_failed";

        String redirectUri = redirectCookieRepository.consumeRedirectUri(request, response);
        String redirectLocation = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", errorCode)
                .build()
                .toUriString();
        response.sendRedirect(redirectLocation);
    }
}
