package com.example.plimap.domain.notification.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    INVALID_CURSOR(
            HttpStatus.BAD_REQUEST,
            "NOTIFICATION_400_INVALID_CURSOR",
            "잘못된 커서 값입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
