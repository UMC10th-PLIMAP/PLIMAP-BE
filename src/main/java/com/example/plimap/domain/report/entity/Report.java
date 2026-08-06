package com.example.plimap.domain.report.entity;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.exception.ReportErrorCode;
import com.example.plimap.domain.report.exception.ReportException;
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
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reporter_member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_report_reporter_member"))
    private Member reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reported_member_id",
            foreignKey = @ForeignKey(name = "fk_report_reported_member"))
    private Member reportedMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reported_pin_id",
            foreignKey = @ForeignKey(name = "fk_report_reported_pin"))
    private Pin reportedPin;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, columnDefinition = "TEXT")
    private ReportCategory category;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    // 관리자가 반려(벌점 미부여)해도 재신고 방지를 위해 row는 유지하고, 이후 조회에서만 제외되도록 표시한다.
    @Column(name = "reviewed", nullable = false)
    private boolean reviewed = false;

    @Builder
    private Report(
            Member reporter,
            Member reportedMember,
            Pin reportedPin,
            ReportCategory category,
            String detail) {
        validateTarget(reporter, reportedMember, reportedPin);
        validateDetail(category, detail);

        this.reporter = reporter;
        this.reportedMember = reportedMember;
        this.reportedPin = reportedPin;
        this.category = category;
        this.detail = detail;
    }

    public static Report createMemberReport(
            Member reporter,
            Member reportedMember,
            ReportCategory category,
            String detail) {
        return Report.builder()
                .reporter(reporter)
                .reportedMember(reportedMember)
                .category(category)
                .detail(detail)
                .build();
    }

    public static Report createPinReport(
            Member reporter,
            Pin reportedPin,
            ReportCategory category,
            String detail) {
        return Report.builder()
                .reporter(reporter)
                .reportedPin(reportedPin)
                .category(category)
                .detail(detail)
                .build();
    }

    private static void validateTarget(Member reporter, Member reportedMember, Pin reportedPin) {
        if (reporter == null) {
            throw new ReportException(ReportErrorCode.REPORT_REPORTER_REQUIRED);
        }
        if ((reportedMember == null) == (reportedPin == null)) {
            throw new ReportException(ReportErrorCode.REPORT_TARGET_INVALID);
        }
        if (reportedMember != null && isSameMember(reporter, reportedMember)) {
            throw new ReportException(ReportErrorCode.REPORT_SELF_NOT_ALLOWED);
        }
    }

    private static void validateDetail(ReportCategory category, String detail) {
        if (category == null) {
            throw new ReportException(ReportErrorCode.REPORT_CATEGORY_REQUIRED);
        }
        if (category == ReportCategory.OTHER && (detail == null || detail.isBlank())) {
            throw new ReportException(ReportErrorCode.REPORT_DETAIL_REQUIRED);
        }
        if (category != ReportCategory.OTHER && detail != null) {
            throw new ReportException(ReportErrorCode.REPORT_DETAIL_NOT_ALLOWED);
        }
    }

    private static boolean isSameMember(Member reporter, Member reportedMember) {
        if (reporter == reportedMember) {
            return true;
        }
        return reporter.getId() != null && reporter.getId().equals(reportedMember.getId());
    }
}
