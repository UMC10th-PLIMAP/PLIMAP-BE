package com.example.plimap.domain.pin.controller.docs;

import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_MESSAGE_SEPARATOR;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_PREFIX;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_SUFFIX;

final class PinSwaggerErrorExamples {

    static final String LOCATION_VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR + "위치 정보가 올바르지 않습니다." + JSON_SUFFIX;
    static final String PAGE_SIZE_VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR + "페이지 크기는 1 이상이어야 합니다." + JSON_SUFFIX;
    static final String LOCATION_DISTANCE_INVALID =
            JSON_PREFIX + "PIN_LOCATION_DISTANCE_INVALID"
                    + JSON_MESSAGE_SEPARATOR
                    + "사용자가 장소 반경이 500m 이상에 있어 PIN을 등록할 수 없습니다."
                    + JSON_SUFFIX;
    static final String MEMBER_PIN_ALREADY_EXISTS =
            JSON_PREFIX + "PIN_MEMBER_PIN_ALREADY_EXISTS"
                    + JSON_MESSAGE_SEPARATOR + "이미 해당 장소에 등록한 핀이 있습니다." + JSON_SUFFIX;
    static final String PIN_NOT_CHANGED =
            JSON_PREFIX + "PIN_NOT_CHANGED"
                    + JSON_MESSAGE_SEPARATOR + "PIN 수정사항이 없습니다." + JSON_SUFFIX;
    static final String INVALID_PIN_OWNER =
            JSON_PREFIX + "PIN_INVALID_PIN_OWNER"
                    + JSON_MESSAGE_SEPARATOR + "해당 PIN에 수정/삭제 권한이 없습니다." + JSON_SUFFIX;
    static final String FRIEND_PIN_ACCESS_DENIED =
            JSON_PREFIX + "PIN_FRIEND_PIN_ACCESS_DENIED"
                    + JSON_MESSAGE_SEPARATOR
                    + "친구가 등록한 핀이 아니므로 접근 권한을 발급할 수 없습니다."
                    + JSON_SUFFIX;
    static final String PIN_ACCESS_DENIED =
            JSON_PREFIX + "PIN_ACCESS_DENIED"
                    + JSON_MESSAGE_SEPARATOR + "접근할 수 없는 핀입니다." + JSON_SUFFIX;
    static final String PIN_NOT_FOUND =
            JSON_PREFIX + "PIN_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "핀을 찾을 수 없습니다." + JSON_SUFFIX;
    static final String TAG_NOT_FOUND =
            JSON_PREFIX + "TAG_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "태그를 찾을 수 없습니다." + JSON_SUFFIX;
    static final String TAG_SIZE_OVER_RANGE =
            JSON_PREFIX + "TAG_SIZE_OVER_RANGE"
                    + JSON_MESSAGE_SEPARATOR + "태그는 최대 4개만 등록 가능합니다." + JSON_SUFFIX;
    static final String PLACE_NOT_FOUND =
            JSON_PREFIX + "PLACE_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "장소를 찾을 수 없습니다." + JSON_SUFFIX;
    static final String PLACE_TRACK_NOT_FOUND =
            JSON_PREFIX + "TRACK_PLACE_TRACK_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "존재하지 않는 placeTrack 입니다." + JSON_SUFFIX;
    static final String YOUTUBE_VIDEO_ID_NOT_FOUND =
            JSON_PREFIX + "TRACK_YOUTUBE_VIDEO_ID_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR
                    + "선택한 곡의 YouTube videoId가 존재하지 않습니다."
                    + JSON_SUFFIX;
    static final String SELECTED_TRACK_CACHE_NOT_FOUND =
            JSON_PREFIX + "TRACK_SELECTED_TRACK_CACHE_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR
                    + "선택한 곡 정보가 만료되었거나 존재하지 않습니다."
                    + JSON_SUFFIX;
    static final String SELECTED_TRACK_CACHE_READER_NOT_AVAILABLE =
            JSON_PREFIX + "TRACK_SELECTED_TRACK_CACHE_READER_NOT_AVAILABLE"
                    + JSON_MESSAGE_SEPARATOR
                    + "선택 곡 캐시 조회 기능을 사용할 수 없습니다."
                    + JSON_SUFFIX;

    private PinSwaggerErrorExamples() {
    }
}
