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
                    "사용자가 장소 반경이 500m 이상에 있어 PIN을 등록할 수 없습니다.ㄴ"
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
