package com.example.plimap.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public final class TokenResolver {

    private TokenResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        // Authorization 헤더 (Swagger 등 API 클라이언트용)
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
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
}
