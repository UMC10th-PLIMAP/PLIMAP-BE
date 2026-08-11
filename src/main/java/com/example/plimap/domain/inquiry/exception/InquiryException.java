package com.example.plimap.domain.inquiry.exception;

import com.example.plimap.global.apiPayload.exception.BusinessException;

public class InquiryException extends BusinessException {

    public InquiryException(InquiryErrorCode errorCode) {
        super(errorCode);
    }

    public InquiryException(InquiryErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
