package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {

    LOGIN(HttpStatus.OK, "MEMBER_LOGIN_SUCCESS", "로그인에 성공했습니다."),
    ONBOARDING_COMPLETED(HttpStatus.OK, "MEMBER_ONBOARDING_COMPLETED_SUCCESS", "온보딩이 완료되었습니다."),
    NICKNAME_CHECKED(HttpStatus.OK, "MEMBER_NICKNAME_CHECKED_SUCCESS", "닉네임 사용 가능 여부를 조회했습니다."),
    LOGOUT(HttpStatus.OK, "MEMBER_LOGOUT_SUCCESS", "로그아웃되었습니다."),
    PROFILE_UPDATED(HttpStatus.OK, "MEMBER_PROFILE_UPDATED_SUCCESS", "프로필이 수정되었습니다."),
    MY_PROFILE_FETCHED(HttpStatus.OK, "MEMBER_MY_PROFILE_FETCHED_SUCCESS", "내 프로필을 조회했습니다."),
    OTHER_PROFILE_FETCHED(HttpStatus.OK, "MEMBER_OTHER_PROFILE_FETCHED_SUCCESS", "회원 프로필을 조회했습니다."),
    FOLLOWED(HttpStatus.OK, "MEMBER_FOLLOWED_SUCCESS", "팔로우했습니다."),
    UNFOLLOWED(HttpStatus.OK, "MEMBER_UNFOLLOWED_SUCCESS", "언팔로우했습니다."),
    FOLLOWERS_FETCHED(HttpStatus.OK, "MEMBER_FOLLOWERS_FETCHED_SUCCESS", "팔로워 목록을 조회했습니다."),
    FOLLOWING_FETCHED(HttpStatus.OK, "MEMBER_FOLLOWING_FETCHED_SUCCESS", "팔로잉 목록을 조회했습니다."),
    PROFILE_IMAGE_UPLOADED(HttpStatus.OK, "MEMBER_PROFILE_IMAGE_UPLOADED_SUCCESS", "프로필 이미지가 업로드되었습니다."),
    PROFILE_IMAGE_REMOVED(HttpStatus.OK, "MEMBER_PROFILE_IMAGE_REMOVED_SUCCESS", "프로필 이미지가 제거되었습니다."),
    WITHDRAWN(HttpStatus.OK, "MEMBER_WITHDRAWN_SUCCESS", "회원 탈퇴가 완료되었습니다."),
    MEMBERS_SEARCHED(HttpStatus.OK, "MEMBER_MEMBERS_SEARCHED_SUCCESS", "회원 검색 결과를 조회했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
