package com.example.plimap.domain.member.dto.response;

import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

public class MemberResDTO {

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
            String profileImageObjectKey,
            Instant updatedAt
    ) {
    }

    public record MyProfile(
            Long id,
            String nickname,
            String name,
            String introduction,
            String profileImageObjectKey,
            long followerCount,
            long followingCount,
            Instant onboardingCompletedAt
    ) {
    }

    public record OtherProfile(
            Long id,
            String nickname,
            String name,
            String introduction,
            String profileImageObjectKey,
            long followerCount,
            long followingCount,
            boolean isFollowing
    ) {
    }

    public record FollowerItem(
            Long id,
            String nickname,
            String name,
            String profileImageObjectKey,
            Instant followedAt,
            boolean isFollowing
    ) {
    }

    public record FollowingItem(
            Long id,
            String nickname,
            String name,
            String profileImageObjectKey,
            Instant followedAt,
            boolean isFollowing
    ) {
    }

    public record ProfileImage(
            String objectKey,
            String imageUrl
    ) {
    }

    public record AdminMe(
            Long id,
            String nickname,
            MemberRole role
    ) {
    }
}
