package com.example.plimap.domain.member.entity;

import com.example.plimap.domain.member.enums.TermsType;
import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "terms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Terms extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TermsType type;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Builder
    private Terms(TermsType type, String version, String title, String content,
                  boolean required, boolean active, Instant effectiveAt) {
        this.type = type;
        this.version = version;
        this.title = title;
        this.content = content;
        this.required = required;
        this.active = active;
        this.effectiveAt = effectiveAt;
    }

    public static Terms create(TermsType type, String version, String title, String content,
                                boolean required, boolean active, Instant effectiveAt) {
        return Terms.builder()
                .type(type)
                .version(version)
                .title(title)
                .content(content)
                .required(required)
                .active(active)
                .effectiveAt(effectiveAt)
                .build();
    }
}
