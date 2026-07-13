package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TermsSuccessCode implements BaseSuccessCode {

    ACTIVE_TERMS_RETRIEVED(HttpStatus.OK, "TERMS_200_ACTIVE_TERMS_RETRIEVED", "활성 약관 목록을 조회했습니다."),
    TERMS_AGREED(HttpStatus.OK, "TERMS_200_TERMS_AGREED", "약관 동의가 완료되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
