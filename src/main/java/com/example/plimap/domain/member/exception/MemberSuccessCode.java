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
    PROFILE_UPDATED(HttpStatus.OK, "MEMBER_200_PROFILE_UPDATED", "프로필이 수정되었습니다."),
    MY_PROFILE_FETCHED(HttpStatus.OK, "MEMBER_200_MY_PROFILE_FETCHED", "내 프로필을 조회했습니다."),
    OTHER_PROFILE_FETCHED(HttpStatus.OK, "MEMBER_200_OTHER_PROFILE_FETCHED", "회원 프로필을 조회했습니다."),
    FOLLOWED(HttpStatus.OK, "MEMBER_200_FOLLOWED", "팔로우했습니다."),
    UNFOLLOWED(HttpStatus.OK, "MEMBER_200_UNFOLLOWED", "언팔로우했습니다."),
    FOLLOWERS_FETCHED(HttpStatus.OK, "MEMBER_200_FOLLOWERS_FETCHED", "팔로워 목록을 조회했습니다."),
    FOLLOWING_FETCHED(HttpStatus.OK, "MEMBER_200_FOLLOWING_FETCHED", "팔로잉 목록을 조회했습니다."),
    PROFILE_IMAGE_UPLOADED(HttpStatus.OK, "MEMBER_200_PROFILE_IMAGE_UPLOADED", "프로필 이미지가 업로드되었습니다."),
    WITHDRAWN(HttpStatus.OK, "MEMBER_200_WITHDRAWN", "회원 탈퇴가 완료되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
