package com.example.plimap.domain.inquiry.entity;

import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "member_id",
            foreignKey = @ForeignKey(name = "fk_inquiry_member"))
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, columnDefinition = "TEXT")
    private InquiryCategory category;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "contact_email", nullable = false, length = 320)
    private String contactEmail;

    @Builder
    private Inquiry(Member member, InquiryCategory category, String title, String content, String contactEmail) {
        this.member = member;
        this.category = category;
        this.title = title;
        this.content = content;
        this.contactEmail = contactEmail;
    }

    public static Inquiry create(Member member, InquiryCategory category, String title, String content, String contactEmail) {
        return Inquiry.builder()
                .member(member)
                .category(category)
                .title(title)
                .content(content)
                .contactEmail(contactEmail)
                .build();
    }
}
