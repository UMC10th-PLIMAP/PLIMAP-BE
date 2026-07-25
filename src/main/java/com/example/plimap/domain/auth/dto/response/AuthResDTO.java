package com.example.plimap.domain.auth.dto.response;

public final class AuthResDTO {

    private AuthResDTO() {
    }

    public record CsrfToken(String token) {
    }
}
