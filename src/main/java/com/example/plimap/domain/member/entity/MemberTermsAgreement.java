package com.example.plimap.domain.member.entity;

import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_terms_agreement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTermsAgreement extends BaseEntity {

    @EmbeddedId
    private MemberTermsAgreementId id;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_terms_agreement_member"))
    private Member member;

    @MapsId("termsId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "terms_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_terms_agreement_terms"))
    private Terms terms;

    @Column(name = "agreed", nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at")
    private Instant agreedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Builder
    private MemberTermsAgreement(Member member, Terms terms, boolean agreed) {
        if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("member must be persisted");
        }
        if (terms == null || terms.getId() == null) {
            throw new IllegalArgumentException("terms must be persisted");
        }

        this.member = member;
        this.terms = terms;
        this.id = new MemberTermsAgreementId(member.getId(), terms.getId());
        this.agreed = agreed;
        this.agreedAt = agreed ? Instant.now() : null;
    }

    public static MemberTermsAgreement create(Member member, Terms terms, boolean agreed) {
        return MemberTermsAgreement.builder()
                .member(member)
                .terms(terms)
                .agreed(agreed)
                .build();
    }

    public void updateAgreement(boolean agreed) {
        this.agreed = agreed;
        this.agreedAt = agreed ? Instant.now() : null;
        if (agreed) {
            this.withdrawnAt = null;
        }
    }

    public void withdraw() {
        this.agreed = false;
        this.agreedAt = null;
        this.withdrawnAt = Instant.now();
    }
}
