package com.example.plimap.domain.place.controller.docs;

import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_MESSAGE_SEPARATOR;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_PREFIX;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_SUFFIX;

final class PlaceSwaggerErrorExamples {

    static final String SCOPE_VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR + "조회 범위가 올바르지 않습니다." + JSON_SUFFIX;
    static final String VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR + "위치 정보가 올바르지 않습니다." + JSON_SUFFIX;
    static final String PLACE_SEARCH_KEYWORD_REQUIRED =
            JSON_PREFIX + "PLACE_SEARCH_KEYWORD_REQUIRED"
                    + JSON_MESSAGE_SEPARATOR + "검색어를 입력해주세요." + JSON_SUFFIX;
    static final String PLACE_CURRENT_LOCATION_REQUIRED =
            JSON_PREFIX + "PLACE_CURRENT_LOCATION_REQUIRED"
                    + JSON_MESSAGE_SEPARATOR + "현재 위치 정보가 필요합니다." + JSON_SUFFIX;
    static final String PLACE_SELECTION_INVALID =
            JSON_PREFIX + "PLACE_SELECTION_INVALID"
                    + JSON_MESSAGE_SEPARATOR + "장소 선택 정보가 올바르지 않습니다." + JSON_SUFFIX;
    static final String PLACE_SEARCH_HISTORY_NOT_FOUND =
            JSON_PREFIX + "PLACE_SEARCH_HISTORY_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "최근 검색 이력을 찾을 수 없습니다." + JSON_SUFFIX;
    static final String PLACE_NOT_FOUND =
            JSON_PREFIX + "PLACE_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "장소를 찾을 수 없습니다." + JSON_SUFFIX;
    static final String PLACE_EXTERNAL_API_ERROR =
            JSON_PREFIX + "PLACE_EXTERNAL_API_ERROR"
                    + JSON_MESSAGE_SEPARATOR + "장소 검색 서비스 연동에 실패했습니다." + JSON_SUFFIX;
    static final String PLACE_EXTERNAL_API_TIMEOUT =
            JSON_PREFIX + "PLACE_EXTERNAL_API_TIMEOUT"
                    + JSON_MESSAGE_SEPARATOR + "장소 검색 서비스 응답이 지연되고 있습니다." + JSON_SUFFIX;

    private PlaceSwaggerErrorExamples() {
    }
}
