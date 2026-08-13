package com.example.plimap.domain.pin.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PinErrorCode implements BaseErrorCode {

    PIN_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PIN_NOT_FOUND",
                    "핀을 찾을 수 없습니다."
    ),

    LOCATION_DISTANCE_INVALID(
            HttpStatus.BAD_REQUEST,
            "PIN_LOCATION_DISTANCE_INVALID",
                    "사용자가 장소 반경이 500m 이상에 있어 PIN을 등록할 수 없습니다."
    ),

    MEMBER_PIN_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "PIN_MEMBER_PIN_ALREADY_EXISTS",
            "이미 해당 장소에 등록한 핀이 있습니다."
    ),

    PIN_NOT_CHANGED(
            HttpStatus.BAD_REQUEST,
            "PIN_NOT_CHANGED",
            "PIN 수정사항이 없습니다."
    ),

    INVALID_PIN_OWNER(
            HttpStatus.FORBIDDEN,
            "PIN_INVALID_PIN_OWNER",
            "해당 PIN에 수정/삭제 권한이 없습니다."
    ),

    PIN_INVALID_CURSOR(
            HttpStatus.BAD_REQUEST,
            "PIN_INVALID_CURSOR",
            "유효하지 않은 커서입니다."
    ),

    FRIEND_PIN_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "PIN_FRIEND_PIN_ACCESS_DENIED",
            "친구가 등록한 핀이 아니므로 접근 권한을 발급할 수 없습니다."
    ),

    PIN_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "PIN_ACCESS_DENIED",
            "접근할 수 없는 핀입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
