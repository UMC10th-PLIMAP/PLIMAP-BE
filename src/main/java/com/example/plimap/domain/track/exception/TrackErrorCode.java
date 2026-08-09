package com.example.plimap.domain.track.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TrackErrorCode implements BaseErrorCode {

    PLACE_TRACK_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PLACE_TRACK_NOT_FOUND",
            "존재하지 않는 placeTrack 입니다."
    ),
    PLACE_TRACK_ALREADY_LIKED(
            HttpStatus.CONFLICT,
            "PLACE_TRACK_ALREADY_LIKED",
            "이미 좋아요를 등록한 장소별 곡입니다."
    ),
    PLACE_TRACK_LIKE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PLACE_TRACK_LIKE_NOT_FOUND",
            "장소별 곡 좋아요를 찾을 수 없습니다."
    ),
    PLACE_TRACK_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "PLACE_TRACK_ACCESS_DENIED",
            "해당 장소의 곡 상세에 접근할 수 없습니다."
    ),
    TRACK_EXTERNAL_API_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "TRACK_EXTERNAL_API_ERROR",
            "음악 검색 중 오류가 발생했습니다."
    ),
    TRACK_METADATA_CACHE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TRACK_404_METADATA_CACHE_NOT_FOUND",
            "곡 메타데이터가 만료되었습니다. 곡을 다시 검색해 주세요."
    ),
    YOUTUBE_MATCH_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TRACK_404_YOUTUBE_MATCH_NOT_FOUND",
            "선택한 곡과 일치하는 YouTube 영상을 찾을 수 없습니다."
    ),
    YOUTUBE_EXTERNAL_API_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "TRACK_500_YOUTUBE_EXTERNAL_API_ERROR",
            "YouTube 영상 검색 중 오류가 발생했습니다."
    ),
    TRACK_CACHE_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "TRACK_500_CACHE_ERROR",
            "곡 캐시 처리 중 오류가 발생했습니다."
    ),

    PLAYBACK_UNAVAILABLE(
            HttpStatus.NOT_FOUND,
            "TRACK_404_PLAYBACK_UNAVAILABLE",
            "최근 재생 실패가 확인된 곡입니다."
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
    SELECTED_TRACK_CACHE_INVALID(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "TRACK_500_SELECTED_TRACK_CACHE_INVALID",
            "선택 곡 캐시 데이터가 올바르지 않습니다."
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
