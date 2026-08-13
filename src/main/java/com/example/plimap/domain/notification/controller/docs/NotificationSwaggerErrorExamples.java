package com.example.plimap.domain.notification.controller.docs;

import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_MESSAGE_SEPARATOR;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_PREFIX;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_SUFFIX;

final class NotificationSwaggerErrorExamples {

    static final String PAGE_SIZE_VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR + "페이지 크기는 1 이상이어야 합니다." + JSON_SUFFIX;
    static final String INVALID_CURSOR =
            JSON_PREFIX + "COMMON_400_INVALID_CURSOR"
                    + JSON_MESSAGE_SEPARATOR + "잘못된 커서 값입니다." + JSON_SUFFIX;

    private NotificationSwaggerErrorExamples() {
    }
}
