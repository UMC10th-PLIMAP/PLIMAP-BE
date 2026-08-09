package com.example.plimap.global.apiPayload.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

import java.util.Objects;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;

    protected BusinessException(BaseErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
        this.errorCode = errorCode;
    }

    protected BusinessException(BaseErrorCode errorCode, Throwable cause) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage(), cause);
        this.errorCode = errorCode;
    }

    protected BusinessException(BaseErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }
}
