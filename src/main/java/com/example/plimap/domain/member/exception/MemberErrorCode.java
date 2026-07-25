package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_404_MEMBER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    NOT_SUPPORT_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "MEMBER_400_NOT_SUPPORT_SOCIAL_PROVIDER", "지원하지 않는 소셜 로그인 제공자입니다."),
    INVALID_SOCIAL_PROFILE(HttpStatus.BAD_REQUEST, "MEMBER_400_INVALID_SOCIAL_PROFILE", "소셜 로그인 제공자로부터 올바른 사용자 정보를 받지 못했습니다."),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "MEMBER_409_NICKNAME_DUPLICATE", "이미 사용 중인 닉네임입니다."),
    ALREADY_ONBOARDED(HttpStatus.CONFLICT, "MEMBER_409_ALREADY_ONBOARDED", "이미 온보딩을 완료한 사용자입니다."),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "MEMBER_400_CANNOT_FOLLOW_SELF", "자기 자신을 팔로우할 수 없습니다."),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "MEMBER_409_ALREADY_FOLLOWING", "이미 팔로우 중인 사용자입니다."),
    NOT_FOLLOWING(HttpStatus.NOT_FOUND, "MEMBER_404_NOT_FOLLOWING", "팔로우 중이 아닌 사용자입니다."),
    CANNOT_UNFOLLOW_SELF(HttpStatus.BAD_REQUEST, "MEMBER_400_CANNOT_UNFOLLOW_SELF", "자기 자신을 언팔로우할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
