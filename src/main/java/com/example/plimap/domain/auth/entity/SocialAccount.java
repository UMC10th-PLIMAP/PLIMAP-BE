package com.example.plimap.domain.auth.entity;

import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "social_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "email", length = 320)
    private String email;

    @Builder
    public SocialAccount(Member member, AuthProvider provider, String providerSubject, String email) {
        this.member = member;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
    }
}
