package com.example.plimap.domain.pin.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import com.example.plimap.global.apiPayload.exception.BusinessException;

public class TagException extends BusinessException {
    public TagException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
