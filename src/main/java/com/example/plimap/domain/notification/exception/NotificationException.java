package com.example.plimap.domain.notification.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import com.example.plimap.global.apiPayload.exception.BusinessException;

public class NotificationException extends BusinessException {

    public NotificationException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(BaseErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public NotificationException(BaseErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
