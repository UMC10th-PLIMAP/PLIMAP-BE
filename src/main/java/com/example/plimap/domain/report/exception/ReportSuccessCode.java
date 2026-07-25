package com.example.plimap.domain.report.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportSuccessCode implements BaseSuccessCode {

    REPORT_CREATE_SUCCESS(
            HttpStatus.CREATED,
            "REPORT_CREATE_SUCCESS",
            "신고가 접수되었습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}