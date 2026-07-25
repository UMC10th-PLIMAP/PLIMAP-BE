package com.example.plimap.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class OAuthFrontendOriginFilter extends OncePerRequestFilter {

    private static final RequestMatcher AUTHORIZATION_REQUEST_MATCHER =
            PathPatternRequestMatcher.withDefaults()
                    .matcher(HttpMethod.GET, "/oauth/authorization/{registrationId}");

    private final OAuthFrontendRedirectCookieRepository redirectCookieRepository;

    public OAuthFrontendOriginFilter(OAuthFrontendRedirectCookieRepository redirectCookieRepository) {
        this.redirectCookieRepository = redirectCookieRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AUTHORIZATION_REQUEST_MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            redirectCookieRepository.validateRequestedOrigin(request);
        } catch (IllegalArgumentException exception) {
            redirectCookieRepository.clearCookie(response);
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "허용되지 않은 OAuth 프론트 Origin입니다."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
