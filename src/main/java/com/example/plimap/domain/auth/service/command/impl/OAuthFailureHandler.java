package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.exception.SanctionedMemberAuthenticationException;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.global.security.OAuthFrontendRedirectCookieRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

        String errorCode = errorCodeFor(exception);

        String redirectUri = redirectCookieRepository.consumeRedirectUri(request, response);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", errorCode);
        if (exception instanceof SanctionedMemberAuthenticationException sanctioned) {
            // FE 안내 모달(정지/탈퇴)에 사유·해제일을 바로 표시할 수 있도록 함께 전달한다.
            if (sanctioned.getReasonCategory() != null) {
                builder.queryParam("reasonCategory", sanctioned.getReasonCategory());
            }
            if (sanctioned.getReasonDetail() != null) {
                builder.queryParam("reasonDetail", sanctioned.getReasonDetail());
            }
            if (sanctioned.getSuspendedUntil() != null) {
                builder.queryParam("suspendedUntil", sanctioned.getSuspendedUntil());
            }
        }
        // reasonDetail은 한글/공백 등 URL에 그대로 쓸 수 없는 문자를 포함할 수 있어 인코딩이 필요하다.
        String redirectLocation = builder.build().encode(StandardCharsets.UTF_8).toUriString();
        response.sendRedirect(redirectLocation);
    }

    private String errorCodeFor(AuthenticationException exception) {
        if (!(exception instanceof SanctionedMemberAuthenticationException sanctioned)) {
            return "oauth_login_failed";
        }
        return sanctioned.getStatus() == MemberStatus.WITHDRAWN
                ? "account_permanently_banned"
                : "account_suspended";
    }
}
