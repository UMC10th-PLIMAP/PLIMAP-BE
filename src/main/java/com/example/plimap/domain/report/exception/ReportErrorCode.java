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
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
