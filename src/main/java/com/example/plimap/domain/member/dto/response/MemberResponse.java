package com.example.plimap.domain.member.dto.response;

import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import com.example.plimap.domain.member.enums.WithdrawalReason;
import com.example.plimap.domain.report.enums.ReportCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

public class MemberResponse {

    @Getter
    @Builder
    public static class Login {
        private String accessToken;
    }

    @Getter
    @Builder
    public static class Onboarding {
        private String nickname;
        private String profileImageObjectKey;
        private Instant onboardingCompletedAt;
    }

    @Getter
    @Builder
    public static class NicknameCheck {
        private String nickname;
        private boolean available;
        private NicknameCheckFailReason reason;
    }

    public record Profile(
            Long id,
            String nickname,
            String name,
            String introduction,
            String profileImageUrl,
            Instant updatedAt
    ) {
    }

    public record MyProfile(
            Long id,
            String nickname,
            String name,
            String introduction,
            String profileImageUrl,
            long followerCount,
            long followingCount,
            Instant onboardingCompletedAt,
            long pinCount,
            MemberStatus status,
            Instant suspendedUntil,
            WithdrawalReason withdrawalReason,
            ReportCategory reasonCategory,
            String reasonDetail
    ) {
    }

    public record OtherProfile(
            Long id,
            String nickname,
            String name,
            String introduction,
            String profileImageUrl,
            long followerCount,
            long followingCount,
            boolean isFollowing,
            boolean isFollowingViewer,
            long pinCount
    ) {
    }

    public record FollowerItem(
            Long id,
            String nickname,
            String name,
            String profileImageUrl,
            Instant followedAt,
            boolean isFollowing,
            boolean isFollowingViewer
    ) {
    }

    public record FollowingItem(
            Long id,
            String nickname,
            String name,
            String profileImageUrl,
            Instant followedAt,
            boolean isFollowing,
            boolean isFollowingViewer
    ) {
    }

    @Schema(name = "MemberSearchItem")
    public record SearchItem(
            Long id,
            String nickname,
            String name,
            String profileImageUrl,
            boolean isFollowing,
            boolean isFollowingViewer,
            Instant joinedAt
    ) {
    }

    public record ProfileImage(
            String objectKey,
            String imageUrl
    ) {
    }
}
