package com.example.plimap.domain.admin.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminSuccessCode implements BaseSuccessCode {

    ME_FETCHED(HttpStatus.OK, "ADMIN_ME_FETCHED_SUCCESS", "관리자 계정 정보를 조회했습니다."),
    PIN_PENALTY_REVIEWED(HttpStatus.OK, "ADMIN_PIN_PENALTY_REVIEWED_SUCCESS", "PIN 신고 검토가 반영되었습니다."),
    PROFILE_PENALTY_REVIEWED(HttpStatus.OK, "ADMIN_PROFILE_PENALTY_REVIEWED_SUCCESS", "프로필 신고 검토가 반영되었습니다."),
    PIN_SANCTION_GRANTED(HttpStatus.OK, "ADMIN_PIN_SANCTION_GRANTED_SUCCESS", "PIN 신고에 대한 최종 제재가 반영되었습니다."),
    MEMBER_SANCTION_GRANTED(HttpStatus.OK, "ADMIN_MEMBER_SANCTION_GRANTED_SUCCESS", "프로필 신고에 대한 최종 제재가 반영되었습니다."),
    REPORTED_PINS_FETCHED(HttpStatus.OK, "ADMIN_REPORTED_PINS_FETCHED_SUCCESS", "신고 누적 게시물 목록을 조회했습니다."),
    MEMBERS_FETCHED(HttpStatus.OK, "ADMIN_MEMBERS_FETCHED_SUCCESS", "회원 목록을 조회했습니다."),
    MEMBER_DETAIL_FETCHED(HttpStatus.OK, "ADMIN_MEMBER_DETAIL_FETCHED_SUCCESS", "회원 상세 정보를 조회했습니다."),
    MEMBER_NICKNAME_REGENERATED(HttpStatus.OK, "ADMIN_MEMBER_NICKNAME_REGENERATED_SUCCESS", "회원 닉네임을 재생성했습니다."),
    INQUIRIES_FETCHED(HttpStatus.OK, "ADMIN_INQUIRIES_FETCHED_SUCCESS", "문의 목록을 조회했습니다."),
    INQUIRY_DETAIL_FETCHED(HttpStatus.OK, "ADMIN_INQUIRY_DETAIL_FETCHED_SUCCESS", "문의 상세 정보를 조회했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
