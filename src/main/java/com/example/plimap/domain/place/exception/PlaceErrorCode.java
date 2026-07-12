package com.example.plimap.domain.place.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements BaseErrorCode {

    PLACE_SEARCH_KEYWORD_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "PLACE_SEARCH_KEYWORD_REQUIRED",
            "검색어를 입력해주세요."
    ),
    PLACE_CURRENT_LOCATION_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "PLACE_CURRENT_LOCATION_REQUIRED",
            "현재 위치 정보가 필요합니다."
    ),
    PLACE_SELECTION_INVALID(
            HttpStatus.BAD_REQUEST,
            "PLACE_SELECTION_INVALID",
            "장소 선택 정보가 올바르지 않습니다."
    ),
    PLACE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PLACE_NOT_FOUND",
            "장소를 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
