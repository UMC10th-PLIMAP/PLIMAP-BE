package com.example.plimap.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public final class TokenResolver {

    private TokenResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        // Authorization 헤더 (Swagger 등 API 클라이언트용) - auth-scheme은 대소문자를 구분하지 않음(RFC 7235)
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return bearer.substring(7).trim();
        }
        // HttpOnly 쿠키 (브라우저용)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String resolveRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
