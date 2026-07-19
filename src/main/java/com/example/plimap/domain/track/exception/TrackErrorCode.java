package com.example.plimap.domain.track.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TrackErrorCode implements BaseErrorCode {

    TRACK_EXTERNAL_API_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "TRACK_EXTERNAL_API_ERROR",
            "음악 검색 중 오류가 발생했습니다."
    ),

    SELECTED_TRACK_CACHE_READER_NOT_AVAILABLE(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "TRACK_500_SELECTED_TRACK_CACHE_READER_NOT_AVAILABLE",
            "선택 곡 캐시 조회 기능을 사용할 수 없습니다."
    ),
    SELECTED_TRACK_CACHE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TRACK_404_SELECTED_TRACK_CACHE_NOT_FOUND",
            "선택한 곡 정보가 만료되었거나 존재하지 않습니다."
    ),
    YOUTUBE_VIDEO_ID_NOT_FOUND(
            HttpStatus.BAD_REQUEST,
            "TRACK_400_YOUTUBE_VIDEO_ID_NOT_FOUND",
            "선택한 곡의 YouTube videoId가 존재하지 않습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
