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
    ),

    PIN_AVAILABILITY_CHECK_SUCCESS(
            HttpStatus.OK,
            "PIN_AVAILABILITY_CHECK_SUCCESS",
            "PIN 등록 가능 여부 검증에 성공했습니다."
    ),

    PIN_UPDATE_SUCCESS(
            HttpStatus.OK,
            "PIN_UPDATE_SUCCESS",
            "PIN이 수정되었습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
