package com.example.plimap.domain.member.dto.response;

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
    }
}
