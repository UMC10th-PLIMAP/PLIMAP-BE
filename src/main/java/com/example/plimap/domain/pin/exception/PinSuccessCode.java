package com.example.plimap.domain.pin.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PinSuccessCode implements BaseSuccessCode {

    PIN_CREATE_SUCCESS(
            HttpStatus.CREATED,
            "PIN_CREATED_SUCCESS",
            "핀이 생성되었습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
