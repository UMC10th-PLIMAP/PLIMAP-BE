package com.example.plimap.domain.track.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TrackSuccessCode implements BaseSuccessCode {

    PLACE_TRACK_DETAIL_SUCCESS(
            HttpStatus.OK,
            "PLACE_TRACK_DETAIL_SUCCESS",
            "장소 노래 상세 정보가 조회되었습니다."
    ),
    TRACK_SEARCH_SUCCESS(
            HttpStatus.OK,
            "TRACK_SEARCH_SUCCESS",
            "음악 검색에 성공했습니다."
    ),
    PLAYBACK_PREPARATION_SUCCESS(
            HttpStatus.OK,
            "TRACK_PLAYBACK_PREPARATION_SUCCESS",
            "구간 재생 준비에 성공했습니다."
    ),
    PLACE_TRACK_LIST_SUCCESS(
            HttpStatus.OK,
            "PLACE_TRACK_LIST_SUCCESS",
            "장소별 곡 목록 조회에 성공했습니다."
    ),
    PLACE_TRACK_LIKE_PUT_SUCCESS(
            HttpStatus.OK,
            "PLACE_TRACK_LIKE_PUT_SUCCESS",
            "장소별 곡 좋아요 등록에 성공했습니다."
    ),
    PLACE_TRACK_LIKE_DELETE_SUCCESS(
            HttpStatus.OK,
            "PLACE_TRACK_LIKE_DELETE_SUCCESS",
            "장소별 곡 좋아요 삭제에 성공했습니다."
    ),
    LIKED_PLACE_TRACK_LIST_SUCCESS(
            HttpStatus.OK,
            "LIKED_PLACE_TRACK_LIST_SUCCESS",
            "좋아요한 곡 목록 조회에 성공했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
