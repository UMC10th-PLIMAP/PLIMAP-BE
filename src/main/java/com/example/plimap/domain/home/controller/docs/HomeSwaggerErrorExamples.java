package com.example.plimap.domain.home.controller.docs;

import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_MESSAGE_SEPARATOR;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_PREFIX;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_SUFFIX;

final class HomeSwaggerErrorExamples {

    static final String LOCATION_VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR + "위치 정보가 올바르지 않습니다." + JSON_SUFFIX;
    static final String PLACE_EXTERNAL_API_ERROR =
            JSON_PREFIX + "PLACE_EXTERNAL_API_ERROR"
                    + JSON_MESSAGE_SEPARATOR + "장소 검색 서비스 연동에 실패했습니다." + JSON_SUFFIX;
    static final String PLACE_EXTERNAL_API_TIMEOUT =
            JSON_PREFIX + "PLACE_EXTERNAL_API_TIMEOUT"
                    + JSON_MESSAGE_SEPARATOR + "장소 검색 서비스 응답이 지연되고 있습니다." + JSON_SUFFIX;

    private HomeSwaggerErrorExamples() {
    }
}
