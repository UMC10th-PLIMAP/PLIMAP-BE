package com.example.plimap.domain.pin.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TagErrorCode implements BaseErrorCode {

    TAG_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TAG_NOT_FOUND",
            "태그를 찾을 수 없습니다."
    ),

    TAG_SIZE_OVER_RANGE(
            HttpStatus.NOT_FOUND,
            "TAG_SIZE_OVER_RANGE",
            "태그는 최대 4개만 등록 가능합니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
