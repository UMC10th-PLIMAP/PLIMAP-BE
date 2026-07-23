package com.example.plimap.domain.report.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {

    REPORT_REPORTER_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "REPORT_REPORTER_REQUIRED",
            "reporter must not be null"
    ),
    REPORT_TARGET_INVALID(
            HttpStatus.BAD_REQUEST,
            "REPORT_TARGET_INVALID",
            "exactly one report target is required"
    ),
    REPORT_SELF_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "REPORT_SELF_NOT_ALLOWED",
            "member cannot report self"
    ),
    REPORT_CATEGORY_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "REPORT_CATEGORY_REQUIRED",
            "category must not be null"
    ),
    REPORT_DETAIL_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "REPORT_DETAIL_REQUIRED",
            "detail is required for OTHER category"
    ),
    REPORT_DETAIL_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "REPORT_DETAIL_NOT_ALLOWED",
            "detail is allowed only for OTHER category"
    ),
    REPORT_MEMBER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "REPORT_MEMBER_ALREADY_EXISTS",
            "이미 신고한 회원입니다."
    ),
    REPORT_PIN_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "REPORT_PIN_ALREADY_EXISTS",
            "이미 신고한 PIN입니다."
    ),
    REPORT_OWN_PIN_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "REPORT_OWN_PIN_NOT_ALLOWED",
            "자신이 작성한 PIN은 신고할 수 없습니다."
    ),
    REPORT_PRIVATE_PIN_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "REPORT_PRIVATE_PIN_NOT_ALLOWED",
            "공개 피드가 아닌 PIN은 신고할 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
