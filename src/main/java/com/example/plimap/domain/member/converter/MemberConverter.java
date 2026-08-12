package com.example.plimap.domain.member.converter;

import com.example.plimap.domain.auth.dto.OAuthDTO;
import com.example.plimap.domain.member.AdminEmailPolicy;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResponse;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import com.example.plimap.domain.member.repository.query.MemberFollowRow;
import com.example.plimap.domain.member.repository.query.MemberSearchRow;

import java.net.URI;
import java.util.List;

public class MemberConverter {

    public static Member toMember(OAuthDTO dto) {
        MemberRole role = AdminEmailPolicy.isAdminEmail(dto.getEmail()) ? MemberRole.ADMIN : null;
        return Member.create(dto.getProvider(), role);
    }

    public static MemberResponse.Login toLogin(String accessToken) {
        return MemberResponse.Login.builder()
                .accessToken(accessToken)
                .build();
    }

    public static MemberResponse.Onboarding toOnboarding(Member member) {
        return MemberResponse.Onboarding.builder()
                .nickname(member.getNickname())
                .profileImageObjectKey(member.getProfileImageObjectKey())
                .onboardingCompletedAt(member.getOnboardingCompletedAt())
                .build();
    }

    public static MemberResponse.NicknameCheck toNicknameCheck(String nickname, NicknameCheckFailReason reason) {
        return MemberResponse.NicknameCheck.builder()
                .nickname(nickname)
                .available(reason == null)
                .reason(reason)
                .build();
    }

    public static MemberResponse.Profile toProfile(Member member, String profileImageUrl) {
        return new MemberResponse.Profile(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getIntroduction(),
                profileImageUrl,
                member.getUpdatedAt()
        );
    }

    public static MemberResponse.MyProfile toMyProfile(Member member, String profileImageUrl, long followerCount, long followingCount, long pinCount) {
        return new MemberResponse.MyProfile(
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

    public static MemberResponse.OtherProfile toOtherProfile(Member member, String profileImageUrl, long followerCount, long followingCount, boolean isFollowing, boolean isFollowingViewer, long pinCount) {
        return new MemberResponse.OtherProfile(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getIntroduction(),
                profileImageUrl,
                followerCount,
                followingCount,
                isFollowing,
                isFollowingViewer,
                pinCount
        );
    }

    public static MemberResponse.ProfileImage toProfileImage(String objectKey, URI imageUrl) {
        return new MemberResponse.ProfileImage(objectKey, imageUrl.toString());
    }

    public static MemberResponse.FollowerItem toFollowerItem(MemberFollowRow row, String profileImageUrl) {
        return new MemberResponse.FollowerItem(
                row.id(),
                row.nickname(),
                row.name(),
                profileImageUrl,
                row.followedAt(),
                row.isFollowing(),
                row.isFollowingViewer()
        );
    }

    public static MemberResponse.FollowingItem toFollowingItem(MemberFollowRow row, String profileImageUrl) {
        return new MemberResponse.FollowingItem(
                row.id(),
                row.nickname(),
                row.name(),
                profileImageUrl,
                row.followedAt(),
                row.isFollowing(),
                row.isFollowingViewer()
        );
    }

    public static MemberResponse.SearchItem toSearchItem(MemberSearchRow row, String profileImageUrl) {
        return new MemberResponse.SearchItem(
                row.id(),
                row.nickname(),
                row.name(),
                profileImageUrl,
                row.isFollowing(),
                row.isFollowingViewer(),
                row.createdAt()
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
