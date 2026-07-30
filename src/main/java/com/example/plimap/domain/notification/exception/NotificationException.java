package com.example.plimap.domain.notification.exception;

import com.example.plimap.global.apiPayload.exception.BusinessException;

public class NotificationException extends BusinessException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(NotificationErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
