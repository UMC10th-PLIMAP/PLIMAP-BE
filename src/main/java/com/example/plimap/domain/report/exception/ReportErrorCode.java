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
            "신고자는 필수입니다."
    ),
    REPORT_TARGET_INVALID(
            HttpStatus.BAD_REQUEST,
            "REPORT_TARGET_INVALID",
            "신고 대상은 회원 또는 PIN 중 하나여야 합니다."
    ),
    REPORT_SELF_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "REPORT_SELF_NOT_ALLOWED",
            "자기 자신은 신고할 수 없습니다."
    ),
    REPORT_CATEGORY_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "REPORT_CATEGORY_REQUIRED",
            "신고 카테고리는 필수입니다."
    ),
    REPORT_DETAIL_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "REPORT_DETAIL_REQUIRED",
            "기타 신고는 상세 내용이 필수입니다."
    ),
    REPORT_DETAIL_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "REPORT_DETAIL_NOT_ALLOWED",
            "상세 내용은 기타 신고에만 입력할 수 있습니다."
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
    ),
    REPORT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "REPORT_404_REPORT_NOT_FOUND",
            "존재하지 않는 신고입니다."
    ),
    REPORT_PIN_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "REPORT_400_REPORT_PIN_MISMATCH",
            "해당 PIN에 대한 신고가 아닙니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
