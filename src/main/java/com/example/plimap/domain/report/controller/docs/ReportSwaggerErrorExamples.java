package com.example.plimap.domain.report.controller.docs;

import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_MESSAGE_SEPARATOR;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_PREFIX;
import static com.example.plimap.global.swagger.CommonSwaggerErrorExamples.JSON_SUFFIX;

final class ReportSwaggerErrorExamples {

    static final String CATEGORY_VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR + "신고 카테고리를 입력해주세요." + JSON_SUFFIX;
    static final String DETAIL_VALIDATION_FAILED =
            JSON_PREFIX + "COMMON_400_VALIDATION_FAILED"
                    + JSON_MESSAGE_SEPARATOR
                    + "신고 상세 내용이 카테고리 조건에 맞지 않습니다."
                    + JSON_SUFFIX;
    static final String REPORT_TARGET_INVALID =
            JSON_PREFIX + "REPORT_TARGET_INVALID"
                    + JSON_MESSAGE_SEPARATOR
                    + "신고 대상은 회원 또는 PIN 중 하나여야 합니다."
                    + JSON_SUFFIX;
    static final String REPORT_SELF_NOT_ALLOWED =
            JSON_PREFIX + "REPORT_SELF_NOT_ALLOWED"
                    + JSON_MESSAGE_SEPARATOR + "자기 자신은 신고할 수 없습니다." + JSON_SUFFIX;
    static final String REPORT_CATEGORY_REQUIRED =
            JSON_PREFIX + "REPORT_CATEGORY_REQUIRED"
                    + JSON_MESSAGE_SEPARATOR + "신고 카테고리는 필수입니다." + JSON_SUFFIX;
    static final String REPORT_DETAIL_REQUIRED =
            JSON_PREFIX + "REPORT_DETAIL_REQUIRED"
                    + JSON_MESSAGE_SEPARATOR + "기타 신고는 상세 내용이 필수입니다." + JSON_SUFFIX;
    static final String REPORT_DETAIL_NOT_ALLOWED =
            JSON_PREFIX + "REPORT_DETAIL_NOT_ALLOWED"
                    + JSON_MESSAGE_SEPARATOR
                    + "상세 내용은 기타 신고에만 입력할 수 있습니다."
                    + JSON_SUFFIX;
    static final String REPORT_MEMBER_ALREADY_EXISTS =
            JSON_PREFIX + "REPORT_MEMBER_ALREADY_EXISTS"
                    + JSON_MESSAGE_SEPARATOR + "이미 신고한 회원입니다." + JSON_SUFFIX;
    static final String REPORT_PIN_ALREADY_EXISTS =
            JSON_PREFIX + "REPORT_PIN_ALREADY_EXISTS"
                    + JSON_MESSAGE_SEPARATOR + "이미 신고한 PIN입니다." + JSON_SUFFIX;
    static final String REPORT_OWN_PIN_NOT_ALLOWED =
            JSON_PREFIX + "REPORT_OWN_PIN_NOT_ALLOWED"
                    + JSON_MESSAGE_SEPARATOR + "자신이 작성한 PIN은 신고할 수 없습니다." + JSON_SUFFIX;
    static final String REPORT_PRIVATE_PIN_NOT_ALLOWED =
            JSON_PREFIX + "REPORT_PRIVATE_PIN_NOT_ALLOWED"
                    + JSON_MESSAGE_SEPARATOR + "공개 피드가 아닌 PIN은 신고할 수 없습니다." + JSON_SUFFIX;
    static final String MEMBER_NOT_FOUND =
            JSON_PREFIX + "MEMBER_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "존재하지 않는 사용자입니다." + JSON_SUFFIX;
    static final String PIN_NOT_FOUND =
            JSON_PREFIX + "PIN_NOT_FOUND"
                    + JSON_MESSAGE_SEPARATOR + "핀을 찾을 수 없습니다." + JSON_SUFFIX;

    private ReportSwaggerErrorExamples() {
    }
}
