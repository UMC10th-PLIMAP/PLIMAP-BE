package com.example.plimap.domain.admin.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminSuccessCode implements BaseSuccessCode {

    ME_FETCHED(HttpStatus.OK, "ADMIN_200_ME_FETCHED", "관리자 계정 정보를 조회했습니다."),
    PIN_PENALTY_REVIEWED(HttpStatus.OK, "ADMIN_200_PIN_PENALTY_REVIEWED", "PIN 신고 검토가 반영되었습니다."),
    PROFILE_PENALTY_REVIEWED(HttpStatus.OK, "ADMIN_200_PROFILE_PENALTY_REVIEWED", "프로필 신고 검토가 반영되었습니다."),
    REPORTED_PINS_FETCHED(HttpStatus.OK, "ADMIN_200_REPORTED_PINS_FETCHED", "신고 누적 게시물 목록을 조회했습니다."),
    MEMBERS_FETCHED(HttpStatus.OK, "ADMIN_200_MEMBERS_FETCHED", "회원 목록을 조회했습니다."),
    MEMBER_DETAIL_FETCHED(HttpStatus.OK, "ADMIN_200_MEMBER_DETAIL_FETCHED", "회원 상세 정보를 조회했습니다."),
    MEMBER_NICKNAME_REGENERATED(HttpStatus.OK, "ADMIN_200_MEMBER_NICKNAME_REGENERATED", "회원 닉네임을 재생성했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
