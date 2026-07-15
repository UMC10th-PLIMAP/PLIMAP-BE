package com.example.plimap.domain.auth.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_INVALID_REFRESH_TOKEN", "유효하지 않거나 만료된 리프레시 토큰입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_401_REFRESH_TOKEN_MISMATCH", "저장된 리프레시 토큰과 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
