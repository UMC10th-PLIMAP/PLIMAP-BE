package com.example.plimap.domain.inquiry.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import com.example.plimap.global.apiPayload.exception.BusinessException;

public class InquiryException extends BusinessException {

    public InquiryException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public InquiryException(BaseErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
