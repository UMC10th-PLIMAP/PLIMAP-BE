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
    PROFILE_PENALTY_REVIEWED(HttpStatus.OK, "ADMIN_200_PROFILE_PENALTY_REVIEWED", "프로필 신고 검토가 반영되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
