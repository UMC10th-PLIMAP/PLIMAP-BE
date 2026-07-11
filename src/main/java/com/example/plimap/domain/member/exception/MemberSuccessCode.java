package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {

    LOGIN(HttpStatus.OK, "MEMBER_200_LOGIN", "로그인에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
