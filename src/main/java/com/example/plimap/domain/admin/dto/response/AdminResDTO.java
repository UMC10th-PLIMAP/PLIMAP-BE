package com.example.plimap.domain.admin.dto.response;

import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.WithdrawalReason;
import com.example.plimap.domain.report.enums.ReportCategory;
import java.time.Instant;
import java.util.List;

public class AdminResDTO {

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
}
