package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_404_1", "존재하지 않는 사용자입니다."),
    NOT_SUPPORT_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "MEMBER_400_3", "지원하지 않는 소셜 로그인 제공자입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
