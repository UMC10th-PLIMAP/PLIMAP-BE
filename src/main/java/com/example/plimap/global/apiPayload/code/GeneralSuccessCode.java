package com.example.plimap.global.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GeneralSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK, "COMMON_OK", "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "COMMON_CREATED", "리소스 생성에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
