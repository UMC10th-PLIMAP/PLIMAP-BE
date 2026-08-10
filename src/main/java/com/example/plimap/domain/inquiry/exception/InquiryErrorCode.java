package com.example.plimap.domain.inquiry.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InquiryErrorCode implements BaseErrorCode {

    INQUIRY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "INQUIRY_404_NOT_FOUND",
            "존재하지 않는 문의입니다."
    ),
    INVALID_CURSOR(
            HttpStatus.BAD_REQUEST,
            "INQUIRY_400_INVALID_CURSOR",
            "유효하지 않은 커서입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
