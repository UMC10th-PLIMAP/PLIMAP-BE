package com.example.plimap.domain.inquiry.controller.docs;

import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_MESSAGE_SEPARATOR;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_PREFIX;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_SUFFIX;

final class InquirySwaggerErrorExamples {

    static final String CATEGORY_VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR + "문의 카테고리를 입력해주세요." + JSON_SUFFIX;
    static final String EMAIL_VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR + "이메일 형식이 올바르지 않습니다." + JSON_SUFFIX;

    private InquirySwaggerErrorExamples() {
    }
}
