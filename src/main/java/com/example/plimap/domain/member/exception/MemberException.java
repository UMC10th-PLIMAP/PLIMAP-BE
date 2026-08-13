package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import com.example.plimap.global.apiPayload.exception.BusinessException;

public class MemberException extends BusinessException {

    public MemberException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public MemberException(BaseErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public MemberException(BaseErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
