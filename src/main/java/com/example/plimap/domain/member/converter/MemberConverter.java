package com.example.plimap.domain.member.converter;

import com.example.plimap.domain.auth.dto.OAuthDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;

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

    public static MemberResDTO.NicknameCheck toNicknameCheck(String nickname, boolean available) {
        return MemberResDTO.NicknameCheck.builder()
                .nickname(nickname)
                .available(available)
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
}
