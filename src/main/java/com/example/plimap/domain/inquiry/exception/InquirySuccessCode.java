package com.example.plimap.domain.inquiry.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InquirySuccessCode implements BaseSuccessCode {

    INQUIRY_CREATE_SUCCESS(
            HttpStatus.CREATED,
            "INQUIRY_CREATE_SUCCESS",
            "문의가 접수되었습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
