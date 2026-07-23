package com.example.plimap.domain.pin.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import com.example.plimap.global.apiPayload.exception.BusinessException;

public class PinLikeException extends BusinessException {
    public PinLikeException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
