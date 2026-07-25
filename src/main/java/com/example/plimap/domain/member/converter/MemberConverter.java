package com.example.plimap.domain.member.converter;

import com.example.plimap.domain.auth.dto.OAuthDTO;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;

import java.net.URI;
import java.util.List;

public class MemberConverter {

    public static Member toMember(OAuthDTO dto) {
        return Member.builder()
                .build();
    }

    public static MemberResDTO.Login toLogin(String accessToken) {
        return MemberResDTO.Login.builder()
                .accessToken(accessToken)
                .build();
    }

    public static MemberResDTO.Onboarding toOnboarding(Member member) {
        return MemberResDTO.Onboarding.builder()
                .nickname(member.getNickname())
                .profileImageObjectKey(member.getProfileImageObjectKey())
                .onboardingCompletedAt(member.getOnboardingCompletedAt())
                .build();
    }

    public static MemberResDTO.NicknameCheck toNicknameCheck(String nickname, NicknameCheckFailReason reason) {
        return MemberResDTO.NicknameCheck.builder()
                .nickname(nickname)
                .available(reason == null)
                .reason(reason)
                .build();
    }

    public static MemberResDTO.Profile toProfile(Member member) {
        return new MemberResDTO.Profile(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getIntroduction(),
                member.getProfileImageObjectKey(),
                member.getUpdatedAt()
        );
    }

    public static MemberResDTO.MyProfile toMyProfile(Member member, long followerCount, long followingCount) {
        return new MemberResDTO.MyProfile(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getIntroduction(),
                member.getProfileImageObjectKey(),
                followerCount,
                followingCount,
                member.getOnboardingCompletedAt()
        );
    }

    public static MemberResDTO.OtherProfile toOtherProfile(Member member, long followerCount, long followingCount, boolean isFollowing) {
        return new MemberResDTO.OtherProfile(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getIntroduction(),
                member.getProfileImageObjectKey(),
                followerCount,
                followingCount,
                isFollowing
        );
    }

    public static MemberResDTO.ProfileImage toProfileImage(String objectKey, URI imageUrl) {
        return new MemberResDTO.ProfileImage(objectKey, imageUrl.toString());
    }

    public static <T> Pagination<T> toPagination(List<T> data, String nextCursor, Boolean hasNext, Integer pageSize) {
        return Pagination.<T>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(pageSize)
                .build();
    }
}
