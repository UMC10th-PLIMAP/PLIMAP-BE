package com.example.plimap.domain.member.entity;

import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.SuspensionPeriod;
import com.example.plimap.domain.member.enums.WithdrawalReason;
import com.example.plimap.domain.report.enums.ReportCategory;
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

    private static final String WITHDRAWN_NICKNAME_PREFIX = "플리맵사용자";
    private static final int AUTO_WITHDRAWAL_THRESHOLD = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", length = 30)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "join_provider", length = 20)
    private AuthProvider joinProvider;

    @Column(name = "penalty_point", nullable = false)
    private Integer penaltyPoint = 0;

    @Column(name = "suspended_until")
    private Instant suspendedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "withdrawal_reason", length = 20)
    private WithdrawalReason withdrawalReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;

    @Column(name = "withdrawn_nickname", length = 10)
    private String withdrawnNickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_penalty_category")
    private ReportCategory lastPenaltyCategory;

    @Column(name = "last_penalty_detail")
    private String lastPenaltyDetail;

    @Builder
    private Member(String nickname, String name, String introduction,
                    String profileImageObjectKey, MemberStatus status,
                    AuthProvider joinProvider, MemberRole role) {
        this.nickname = nickname;
        this.name = name;
        this.introduction = introduction;
        this.profileImageObjectKey = profileImageObjectKey;
        this.status = status != null ? status : MemberStatus.ACTIVE;
        this.joinProvider = joinProvider;
        this.role = role != null ? role : MemberRole.USER;
    }

    public static Member create(AuthProvider joinProvider, MemberRole role) {
        return Member.builder()
                .joinProvider(joinProvider)
                .role(role)
                .build();
    }

    public void grantAdmin() {
        this.role = MemberRole.ADMIN;
    }

    public boolean isOnboarded() {
        return onboardingCompletedAt != null;
    }

    public void completeOnboarding(String nickname) {
        this.nickname = nickname;
        this.onboardingCompletedAt = Instant.now();
    }

    public void updateProfile(String nickname, String name, String introduction) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (name != null) {
            this.name = name.isEmpty() ? null : name;
        }
        if (introduction != null) {
            this.introduction = introduction;
        }
    }

    public void updateProfileImage(String profileImageObjectKey) {
        if (profileImageObjectKey == null) {
            throw new IllegalArgumentException("profileImageObjectKey must not be null");
        }
        this.profileImageObjectKey = profileImageObjectKey;
    }

    public void removeProfileImage() {
        this.profileImageObjectKey = null;
    }

    public void withdrawVoluntarily() {
        this.withdrawnNickname = this.nickname;
        this.nickname = WITHDRAWN_NICKNAME_PREFIX + this.id;
        this.introduction = null;
        this.profileImageObjectKey = null;
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawalReason = WithdrawalReason.VOLUNTARY;
        delete();
    }

    public String getDisplayNickname() {
        return status == MemberStatus.WITHDRAWN ? WITHDRAWN_NICKNAME_PREFIX : nickname;
    }

    public void applySanction(SuspensionPeriod period, ReportCategory reasonCategory, String reasonDetail) {
        this.penaltyPoint += 1;
        this.lastPenaltyCategory = reasonCategory;
        this.lastPenaltyDetail = reasonDetail;

        if (period.isPermanent() || penaltyPoint >= AUTO_WITHDRAWAL_THRESHOLD) {
            withdrawByPenalty();
        } else {
            this.status = MemberStatus.SUSPENDED;
            this.suspendedUntil = Instant.now().plus(period.getDuration());
        }
    }

    public void withdrawByPenalty() {
        this.withdrawnNickname = this.nickname;
        this.nickname = WITHDRAWN_NICKNAME_PREFIX + this.id;
        this.introduction = null;
        this.profileImageObjectKey = null;
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawalReason = WithdrawalReason.PENALTY;
        this.suspendedUntil = null;
        delete();
    }

    public void liftSuspension() {
        this.status = MemberStatus.ACTIVE;
        this.suspendedUntil = null;
    }

    public void replaceNicknameForPenalty(String newNickname) {
        this.nickname = newNickname;
        this.reportCount = 0;
    }

    public void resetReportCount() {
        this.reportCount = 0;
    }

    public void regenerateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public boolean isSuspensionExpired() {
        return status == MemberStatus.SUSPENDED
                && suspendedUntil != null
                && !suspendedUntil.isAfter(Instant.now());
    }
}
