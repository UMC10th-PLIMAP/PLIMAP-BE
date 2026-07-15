package com.example.plimap.domain.auth.exception;

import com.example.plimap.global.apiPayload.exception.BusinessException;

public class AuthException extends BusinessException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
