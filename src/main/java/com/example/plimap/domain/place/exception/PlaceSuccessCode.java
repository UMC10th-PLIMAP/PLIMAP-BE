package com.example.plimap.domain.place.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceSuccessCode implements BaseSuccessCode {

    PLACE_SEARCH_HISTORY_LIST_SUCCESS(
            HttpStatus.OK,
            "PLACE_SEARCH_HISTORY_LIST_SUCCESS",
            "최근 검색 장소 목록 조회에 성공했습니다."
    ),
    PLACE_SEARCH_HISTORY_DELETE_SUCCESS(
            HttpStatus.OK,
            "PLACE_SEARCH_HISTORY_DELETE_SUCCESS",
            "최근 검색 장소 삭제에 성공했습니다."
    ),
    PLACE_SEARCH_SUCCESS(
            HttpStatus.OK,
            "PLACE_SEARCH_SUCCESS",
            "장소 검색에 성공했습니다."
    ),
    PLACE_SELECTION_SUCCESS(
            HttpStatus.OK,
            "PLACE_SELECTION_SUCCESS",
            "장소 선택에 성공했습니다."
    ),
    PLACE_MAP_SELECTION_SUCCESS(
            HttpStatus.OK,
            "PLACE_MAP_SELECTION_SUCCESS",
            "지도 선택 장소 판정에 성공했습니다."
    ),
    PLACE_DETAIL_SUCCESS(
            HttpStatus.OK,
            "PLACE_DETAIL_SUCCESS",
            "장소 상세 조회에 성공했습니다."
    ),
    PLACE_BOOKMARK_CREATE_SUCCESS(
            HttpStatus.OK,
            "PLACE_BOOKMARK_CREATE_SUCCESS",
            "장소 북마크 등록에 성공했습니다."
    ),
    PLACE_BOOKMARK_DELETE_SUCCESS(
            HttpStatus.OK,
            "PLACE_BOOKMARK_DELETE_SUCCESS",
            "장소 북마크 삭제에 성공했습니다."
    ),
    PLACE_BOOKMARK_LIST_SUCCESS(
            HttpStatus.OK,
            "PLACE_BOOKMARK_LIST_SUCCESS",
            "저장한 장소 목록 조회에 성공했습니다."
    ),
    PLACE_POPULAR_LIST_SUCCESS(
            HttpStatus.OK,
            "PLACE_POPULAR_LIST_SUCCESS",
            "인기 장소 목록 조회에 성공했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
