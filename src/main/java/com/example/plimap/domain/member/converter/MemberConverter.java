package com.example.plimap.domain.member.converter;

import com.example.plimap.domain.auth.dto.OAuthDTO;
import com.example.plimap.domain.member.AdminEmailPolicy;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import com.example.plimap.domain.member.repository.query.MemberFollowRow;

import java.net.URI;
import java.util.List;

public class MemberConverter {

    public static Member toMember(OAuthDTO dto) {
        MemberRole role = AdminEmailPolicy.isAdminEmail(dto.getEmail()) ? MemberRole.ADMIN : null;
        return Member.create(dto.getProvider(), role);
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

    public static MemberResDTO.Profile toProfile(Member member, String profileImageUrl) {
        return new MemberResDTO.Profile(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getIntroduction(),
                profileImageUrl,
                member.getUpdatedAt()
        );
    }

    public static MemberResDTO.MyProfile toMyProfile(Member member, String profileImageUrl, long followerCount, long followingCount, long pinCount) {
        return new MemberResDTO.MyProfile(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getIntroduction(),
                profileImageUrl,
                followerCount,
                followingCount,
                member.getOnboardingCompletedAt(),
                pinCount
        );
    }

    public static MemberResDTO.OtherProfile toOtherProfile(Member member, String profileImageUrl, long followerCount, long followingCount, boolean isFollowing, long pinCount) {
        return new MemberResDTO.OtherProfile(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getIntroduction(),
                profileImageUrl,
                followerCount,
                followingCount,
                isFollowing,
                pinCount
        );
    }

    public static MemberResDTO.ProfileImage toProfileImage(String objectKey, URI imageUrl) {
        return new MemberResDTO.ProfileImage(objectKey, imageUrl.toString());
    }

    public static MemberResDTO.FollowerItem toFollowerItem(MemberFollowRow row, String profileImageUrl) {
        return new MemberResDTO.FollowerItem(
                row.id(),
                row.nickname(),
                row.name(),
                profileImageUrl,
                row.followedAt(),
                row.isFollowing(),
                row.isFollowingViewer()
        );
    }

    public static MemberResDTO.FollowingItem toFollowingItem(MemberFollowRow row, String profileImageUrl) {
        return new MemberResDTO.FollowingItem(
                row.id(),
                row.nickname(),
                row.name(),
                profileImageUrl,
                row.followedAt(),
                row.isFollowing(),
                row.isFollowingViewer()
        );
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
