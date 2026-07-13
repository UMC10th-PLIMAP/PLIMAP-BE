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
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
