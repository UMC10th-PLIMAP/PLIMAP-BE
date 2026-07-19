package com.example.plimap.domain.track.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TrackSuccessCode implements BaseSuccessCode {

    TRACK_SEARCH_SUCCESS(
            HttpStatus.OK,
            "TRACK_SEARCH_SUCCESS",
            "음악 검색에 성공했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
