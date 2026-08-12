package com.example.plimap.domain.admin.dto.response;

import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.WithdrawalReason;
import com.example.plimap.domain.report.enums.ReportCategory;
import java.time.Instant;
import java.util.List;

public class AdminResponse {

    public record Me(
            Long id,
            String nickname,
            MemberRole role
    ) {
        public static Me from(Member member) {
            return new Me(member.getId(), member.getNickname(), member.getRole());
        }
    }

    public record ReportReasonItem(
            Long reportId,
            ReportCategory category,
            String detail,
            String reporterNickname,
            Instant createdAt
    ) {
    }

    public record ReportedPinItem(
            Long pinId,
            String pinTitle,
            String pinLocation,
            Long authorMemberId,
            String authorNickname,
            MemberStatus authorStatus,
            int reportCount,
            boolean autoHidden,
            Instant createdAt,
            List<ReportReasonItem> reasons
    ) {
    }

    public record ReportedPinPage(
            List<ReportedPinItem> items,
            long total,
            int page,
            int pageSize
    ) {
    }

    public record MemberSummary(
            Long id,
            String nickname,
            String name,
            MemberStatus status,
            AuthProvider joinProvider,
            int penaltyPoint,
            Instant suspendedUntil,
            Instant createdAt
    ) {
        public static MemberSummary from(Member member) {
            return new MemberSummary(
                    member.getId(),
                    member.getNickname(),
                    member.getName(),
                    member.getStatus(),
                    member.getJoinProvider(),
                    member.getPenaltyPoint(),
                    member.getSuspendedUntil(),
                    member.getCreatedAt()
            );
        }
    }

    public record MemberPage(
            List<MemberSummary> items,
            long total,
            int page,
            int pageSize
    ) {
    }

    public record MemberDetail(
            Long id,
            String nickname,
            String name,
            String email,
            MemberStatus status,
            MemberRole role,
            AuthProvider joinProvider,
            int penaltyPoint,
            Instant suspendedUntil,
            WithdrawalReason withdrawalReason,
            Instant createdAt
    ) {
        public static MemberDetail from(Member member, String email) {
            return new MemberDetail(
                    member.getId(),
                    member.getNickname(),
                    member.getName(),
                    email,
                    member.getStatus(),
                    member.getRole(),
                    member.getJoinProvider(),
                    member.getPenaltyPoint(),
                    member.getSuspendedUntil(),
                    member.getWithdrawalReason(),
                    member.getCreatedAt()
            );
        }
    }

    public record InquirySummary(
            Long id,
            InquiryCategory category,
            String title,
            Long memberId,
            String memberNickname,
            String contactEmail,
            Instant createdAt
    ) {
        public static InquirySummary from(Inquiry inquiry) {
            Member member = inquiry.getMember();
            return new InquirySummary(
                    inquiry.getId(),
                    inquiry.getCategory(),
                    inquiry.getTitle(),
                    member != null ? member.getId() : null,
                    member != null ? member.getNickname() : null,
                    inquiry.getContactEmail(),
                    inquiry.getCreatedAt()
            );
        }
    }

    public record InquiryPage(
            List<InquirySummary> items,
            String nextCursor,
            boolean hasNext,
            int pageSize
    ) {
    }

    public record InquiryDetail(
            Long id,
            InquiryCategory category,
            String title,
            String content,
            Long memberId,
            String memberNickname,
            String contactEmail,
            Instant createdAt
    ) {
        public static InquiryDetail from(Inquiry inquiry) {
            Member member = inquiry.getMember();
            return new InquiryDetail(
                    inquiry.getId(),
                    inquiry.getCategory(),
                    inquiry.getTitle(),
                    inquiry.getContent(),
                    member != null ? member.getId() : null,
                    member != null ? member.getNickname() : null,
                    inquiry.getContactEmail(),
                    inquiry.getCreatedAt()
            );
        }
    }
}
