package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {

    LOGIN(HttpStatus.OK, "MEMBER_200_LOGIN", "로그인에 성공했습니다."),
    ONBOARDING_COMPLETED(HttpStatus.OK, "MEMBER_200_ONBOARDING_COMPLETED", "온보딩이 완료되었습니다."),
    NICKNAME_CHECKED(HttpStatus.OK, "MEMBER_200_NICKNAME_CHECKED", "닉네임 사용 가능 여부를 조회했습니다."),
    LOGOUT(HttpStatus.OK, "MEMBER_200_LOGOUT", "로그아웃되었습니다."),
    FOLLOWED(HttpStatus.OK, "MEMBER_200_FOLLOWED", "팔로우했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
