package com.example.plimap.domain.notification.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationSuccessCode implements BaseSuccessCode {

    NOTIFICATIONS_RETRIEVED(
            HttpStatus.OK,
            "NOTIFICATION_NOTIFICATIONS_RETRIEVED_SUCCESS",
            "알림 목록을 조회했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
