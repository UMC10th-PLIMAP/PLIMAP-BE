package com.example.plimap.domain.auth.dto.response;

public final class AuthResponse {

    private AuthResponse() {
    }

    public record CsrfToken(String token) {
    }
}
