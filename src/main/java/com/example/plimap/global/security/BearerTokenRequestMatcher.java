package com.example.plimap.global.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class BearerTokenRequestMatcher implements RequestMatcher {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean matches(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null
                && authorization.startsWith(BEARER_PREFIX)
                && authorization.length() > BEARER_PREFIX.length();
    }
}
