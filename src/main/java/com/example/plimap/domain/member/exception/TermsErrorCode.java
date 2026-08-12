package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TermsErrorCode implements BaseErrorCode {

    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "TERMS_NOT_FOUND", "해당 유형의 활성 약관을 찾을 수 없습니다."),
    AGREEMENT_REQUIRED(HttpStatus.BAD_REQUEST, "TERMS_AGREEMENT_REQUIRED", "필수 약관에 모두 동의해야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
