package com.example.plimap.domain.home.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HomeSuccessCode implements BaseSuccessCode {

    HOME_200_CONTEXT_FETCHED(
            HttpStatus.OK,
            "HOME_200_CONTEXT_FETCHED",
            "홈 컨텍스트를 조회했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
