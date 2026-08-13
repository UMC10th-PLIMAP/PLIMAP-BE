package com.example.plimap.global.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GeneralErrorCode implements BaseErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST,
            "COMMON_400_BAD_REQUEST",
            "잘못된 요청입니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST,
            "COMMON_400_VALIDATION_FAILED",
            "요청 값이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST,
            "COMMON_400_MISSING_PARAMETER",
            "필수 요청 파라미터가 누락되었습니다."),
    MISSING_HEADER(HttpStatus.BAD_REQUEST,
            "COMMON_400_MISSING_HEADER",
            "필수 요청 헤더가 누락되었습니다."),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST,
            "COMMON_400_TYPE_MISMATCH",
            "요청 값의 타입이 올바르지 않습니다."),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST,
            "COMMON_400_MALFORMED_JSON",
            "요청 본문의 형식이 올바르지 않습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST,
            "COMMON_400_INVALID_CURSOR",
            "유효하지 않은 커서입니다."),
    CONTENT_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE,
            "COMMON_413_CONTENT_TOO_LARGE",
            "요청 크기가 허용된 한도를 초과했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,
            "COMMON_401_UNAUTHORIZED",
            "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,
            "COMMON_403_FORBIDDEN",
            "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "COMMON_404_NOT_FOUND",
            "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,
            "COMMON_405_METHOD_NOT_ALLOWED",
            "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON_500_INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
