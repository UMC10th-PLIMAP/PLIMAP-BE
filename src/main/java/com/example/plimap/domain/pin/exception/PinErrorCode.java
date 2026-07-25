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
            "LOCATION_DISTANCE_INVALID",
                    "사용자가 장소 반경이 500m 이상에 있어 PIN을 등록할 수 없습니다."
    ),

    MEMBER_PIN_ALREADY_EXISTS(
            HttpStatus.BAD_REQUEST,
            "MEMBER_PIN_ALREADY_EXISTS",
            "이미 해당 장소에 등록한 핀이 있습니다."
    ),

    PIN_NOT_CHANGED(
            HttpStatus.BAD_REQUEST,
            "PIN_NOT_CHANGED",
            "PIN 수정사항이 없습니다."
    ),

    INVALID_PIN_OWNER(
            HttpStatus.FORBIDDEN,
            "INVALID_PIN_OWNER",
            "해당 PIN에 수정/삭제 권한이 없습니다."
    ),

    ALREADY_LIKED_PIN(
            HttpStatus.BAD_REQUEST,
            "ALREADY_LIKED_PIN",
            "이미 좋아요한 핀입니다."
    ),

    INVALID_CURSOR(
            HttpStatus.BAD_REQUEST,
            "INVALID_CURSOR",
            "유효하지 않은 커서입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
