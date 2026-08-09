package com.example.plimap.domain.auth.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements BaseSuccessCode {

    TOKEN_REISSUED(HttpStatus.OK, "AUTH_200_TOKEN_REISSUED", "토큰이 재발급되었습니다."),
    CSRF_TOKEN_ISSUED(HttpStatus.OK, "AUTH_200_CSRF_TOKEN_ISSUED", "CSRF 토큰이 발급되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
