package com.example.plimap.domain.admin.controller.docs;

import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_MESSAGE_SEPARATOR;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_PREFIX;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_SUFFIX;

final class AdminSwaggerErrorExamples {

    static final String PENALTY_GRANT_NOT_SUPPORTED =
            JSON_PREFIX + "ADMIN_PENALTY_GRANT_NOT_SUPPORTED"
                    + JSON_MESSAGE_SEPARATOR
                    + "벌점 부여는 이 API에서 더 이상 지원하지 않습니다. 최종 제재 API를 이용해 주세요."
                    + JSON_SUFFIX;
    static final String REPORT_PIN_MISMATCH =
            JSON_PREFIX + "REPORT_PIN_MISMATCH"
                    + JSON_MESSAGE_SEPARATOR + "해당 PIN에 대한 신고가 아닙니다." + JSON_SUFFIX;
    static final String REPORT_NOT_FOUND =
            JSON_PREFIX + "REPORT_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "존재하지 않는 신고입니다." + JSON_SUFFIX;
    static final String PIN_NOT_FOUND =
            JSON_PREFIX + "PIN_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "핀을 찾을 수 없습니다." + JSON_SUFFIX;
    static final String MEMBER_NOT_FOUND =
            JSON_PREFIX + "MEMBER_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "존재하지 않는 사용자입니다." + JSON_SUFFIX;
    static final String INQUIRY_NOT_FOUND =
            JSON_PREFIX + "INQUIRY_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "존재하지 않는 문의입니다." + JSON_SUFFIX;
    static final String PENALTY_NICKNAME_POOL_EXHAUSTED =
            JSON_PREFIX + "MEMBER_PENALTY_NICKNAME_POOL_EXHAUSTED"
                    + JSON_MESSAGE_SEPARATOR
                    + "벌점 부여용 닉네임 후보를 찾지 못했습니다."
                    + JSON_SUFFIX;

    private AdminSwaggerErrorExamples() {
    }
}
