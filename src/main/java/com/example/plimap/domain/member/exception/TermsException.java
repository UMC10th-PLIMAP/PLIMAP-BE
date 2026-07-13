package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.exception.BusinessException;

public class TermsException extends BusinessException {

    public TermsException(TermsErrorCode errorCode) {
        super(errorCode);
    }

    public TermsException(TermsErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
