package com.example.plimap.domain.member.entity;

import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.global.entity.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", length = 10)
    private String nickname;

    @Column(name = "name", length = 7)
    private String name;

    @Column(name = "introduction", length = 100)
    private String introduction;

    @Column(name = "profile_image_object_key", length = 500)
    private String profileImageObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Builder
    public Member(String nickname, String name, String introduction,
                  String profileImageObjectKey, MemberStatus status) {
        this.nickname = nickname;
        this.name = name;
        this.introduction = introduction;
        this.profileImageObjectKey = profileImageObjectKey;
        this.status = status != null ? status : MemberStatus.ACTIVE;
    }

    public boolean isOnboarded() {
        return onboardingCompletedAt != null;
    }

    public void completeOnboarding(String nickname, String profileImageObjectKey) {
        this.nickname = nickname;
        this.profileImageObjectKey = profileImageObjectKey;
        this.onboardingCompletedAt = Instant.now();
    }

    public void updateProfile(String nickname, String name, String introduction, String profileImageObjectKey) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (name != null) {
            this.name = name;
        }
        if (introduction != null) {
            this.introduction = introduction;
        }
        if (profileImageObjectKey != null) {
            this.profileImageObjectKey = profileImageObjectKey;
        }
    }
}
