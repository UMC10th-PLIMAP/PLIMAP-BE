package com.example.plimap.domain.admin.service.command.impl;

import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.admin.exception.AdminErrorCode;
import com.example.plimap.domain.admin.exception.AdminException;
import com.example.plimap.domain.admin.service.command.AdminCommandService;
import com.example.plimap.domain.auth.service.query.AuthQueryService;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.SuspensionPeriod;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.notification.service.command.NotificationCommandService;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.report.dto.ReportReason;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.exception.ReportErrorCode;
import com.example.plimap.domain.report.exception.ReportException;
import com.example.plimap.domain.report.service.command.ReportCommandService;
import com.example.plimap.domain.report.service.query.ReportQueryService;
import com.example.plimap.domain.track.service.command.PlaceTrackCommandService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * report/pin/member/notification/track 도메인 Service만 조합하는 최상위 오케스트레이터.
 * report 도메인이 이미 member/pin 도메인을 참조하고 있어, 캐스케이드 삭제 로직을 member나 pin
 * 도메인 안에 두면 순환 빈 의존이 생긴다 — 아무도 의존하지 않는 admin 도메인에 둬서 회피한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCommandServiceImpl implements AdminCommandService {

    private final PinQueryService pinQueryService;
    private final PinCommandService pinCommandService;
    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;
    private final NotificationCommandService notificationCommandService;
    private final ReportCommandService reportCommandService;
    private final ReportQueryService reportQueryService;
    private final PlaceTrackCommandService placeTrackCommandService;
    private final AuthQueryService authQueryService;

    @Override
    public void reviewPinReport(Long pinId, boolean grantPenalty) {
        if (grantPenalty) {
            // 벌점 부여는 사유(어떤 신고를 근거로 삼는지)·기간을 admin이 직접 고르는
            // grantPinSanction()으로 이전됐다. 이 API는 반려 전용으로만 남는다.
            throw new AdminException(AdminErrorCode.PENALTY_GRANT_NOT_SUPPORTED);
        }
        pinCommandService.resetPinReportCount(pinId);
        reportCommandService.markPinReportsReviewed(pinId);
    }

    @Override
    public void reviewProfileReport(Long memberId, boolean grantPenalty) {
        if (grantPenalty) {
            // 벌점 부여는 admin이 사유(category/detail)·기간을 직접 작성하는
            // grantMemberSanction()으로 이전됐다. 이 API는 반려 전용으로만 남는다.
            throw new AdminException(AdminErrorCode.PENALTY_GRANT_NOT_SUPPORTED);
        }
        memberCommandService.resetReportCount(memberId);
    }

    @Override
    public void grantPinSanction(Long pinId, Long reportId, SuspensionPeriod period) {
        ReportReason reason = reportQueryService.getReasonById(reportId);
        if (!pinId.equals(reason.pinId())) {
            throw new ReportException(ReportErrorCode.REPORT_PIN_MISMATCH);
        }

        Pin pin = pinQueryService.getActivePin(pinId);
        Long ownerId = pin.getMember().getId();
        pinCommandService.penalizePin(pinId);

        if (memberCommandService.applySanction(ownerId, period, reason.category(), reason.detail())) {
            cascadeAutoWithdrawal(ownerId);
        }
    }

    @Override
    public void grantMemberSanction(Long memberId, ReportCategory reasonCategory, String reasonDetail, SuspensionPeriod period) {
        validateReasonDetail(reasonCategory, reasonDetail);

        String newNickname = memberQueryService.pickAvailablePenaltyNickname();
        memberCommandService.replacePenalizedNickname(memberId, newNickname);

        if (memberCommandService.applySanction(memberId, period, reasonCategory, reasonDetail)) {
            cascadeAutoWithdrawal(memberId);
        }
    }

    private void validateReasonDetail(ReportCategory category, String detail) {
        if (category == ReportCategory.OTHER && (detail == null || detail.isBlank())) {
            throw new ReportException(ReportErrorCode.REPORT_DETAIL_REQUIRED);
        }
        if (category != ReportCategory.OTHER && detail != null) {
            throw new ReportException(ReportErrorCode.REPORT_DETAIL_NOT_ALLOWED);
        }
    }

    @Override
    public AdminResDTO.MemberDetail regenerateMemberNickname(Long memberId) {
        String newNickname = memberQueryService.pickAvailablePenaltyNickname();
        memberCommandService.regenerateNickname(memberId, newNickname);

        Member updated = memberQueryService.getMemberById(memberId);
        String email = authQueryService.findEmailByMemberId(memberId).orElse(null);
        return AdminResDTO.MemberDetail.from(updated, email);
    }

    private void cascadeAutoWithdrawal(Long memberId) {
        List<Long> pinIds = pinQueryService.findAllPinIdsByMemberId(memberId);

        // notification.pin_id는 ON DELETE RESTRICT라 핀을 하드삭제하기 전에 참조 row를 먼저 지워야 한다.
        // deleteByMemberId는 이 회원이 actor/recipient인 알림(팔로우 알림 포함)을 전부 지우는데,
        // 이 회원 소유 핀에 달린 알림(PIN_CREATED는 actor, PIN_LIKED는 recipient가 항상 이 회원)도
        // 그 안에 포함되므로 별도로 pin_id 기준 삭제를 할 필요가 없다.
        notificationCommandService.deleteByMemberId(memberId);
        // report.reported_pin_id도 ON DELETE RESTRICT라 마찬가지로 먼저 지운다.
        reportCommandService.deleteReportsByPinIds(pinIds);
        // 이 회원이 신고당한 이력뿐 아니라, 이 회원이 다른 대상을 신고한 이력도 함께 지운다
        // (자동 탈퇴는 자발적 탈퇴와 달리 기록을 남기지 않는다는 설계 원칙).
        reportCommandService.deleteReportsAgainstMember(memberId);
        reportCommandService.deleteReportsByReporter(memberId);

        // 이 회원이 다른 회원의 핀에 누른 좋아요(pin_like)도 하드삭제해야 한다. Member는 soft
        // delete라 fk_pin_like_member의 ON DELETE CASCADE가 실행되지 않으므로 직접 지워야 하고,
        // 삭제 전에 좋아요 대상 핀의 likeCount도 먼저 보정해야 한다(reportCount와 동일한 이유).
        // 이 회원 소유 핀에 대한 좋아요는 어차피 그 핀이 곧 하드삭제되므로 보정이 무의미하지만,
        // 걸러내지 않아도 해가 없어 그대로 둔다.
        pinQueryService.findPinIdsLikedByMember(memberId).forEach(pinCommandService::decreaseLikeCount);
        pinCommandService.hardDeleteLikesByMember(memberId);

        pinCommandService.hardDeleteAllByMember(memberId);
        placeTrackCommandService.hardDeleteLikesByMember(memberId);

        // 팔로우 하드삭제 + SocialAccount 유지(재가입 영구 차단)는
        // memberCommandService.applySanction() 안에서 이미 처리됨.
    }
}
