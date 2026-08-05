package com.example.plimap.domain.admin.service.command.impl;

import com.example.plimap.domain.admin.service.command.AdminCommandService;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.notification.service.command.NotificationCommandService;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.report.service.command.ReportCommandService;
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
    private final PlaceTrackCommandService placeTrackCommandService;

    @Override
    public void reviewPinReport(Long pinId, boolean grantPenalty) {
        if (!grantPenalty) {
            pinCommandService.resetPinReportCount(pinId);
            return;
        }

        Pin pin = pinQueryService.getActivePin(pinId);
        Long ownerId = pin.getMember().getId();
        pinCommandService.penalizePin(pinId);

        if (memberCommandService.increasePenaltyPoint(ownerId)) {
            cascadeAutoWithdrawal(ownerId);
        }
    }

    @Override
    public void reviewProfileReport(Long memberId, boolean grantPenalty) {
        if (!grantPenalty) {
            memberCommandService.resetReportCount(memberId);
            return;
        }

        String newNickname = memberQueryService.pickAvailablePenaltyNickname();
        memberCommandService.replacePenalizedNickname(memberId, newNickname);

        if (memberCommandService.increasePenaltyPoint(memberId)) {
            cascadeAutoWithdrawal(memberId);
        }
    }

    private void cascadeAutoWithdrawal(Long memberId) {
        List<Long> pinIds = pinQueryService.findAllPinIdsByMemberId(memberId);

        // report.reported_pin_id / notification.pin_id는 ON DELETE RESTRICT라
        // 핀을 하드삭제하기 전에 참조 row를 먼저 지워야 한다.
        notificationCommandService.deleteByPinIds(pinIds);
        reportCommandService.deleteReportsByPinIds(pinIds);
        reportCommandService.deleteReportsAgainstMember(memberId);

        pinCommandService.hardDeleteAllByMember(memberId);
        placeTrackCommandService.hardDeleteLikesByMember(memberId);

        // 팔로우 하드삭제 + SocialAccount 유지(재가입 영구 차단)는
        // memberCommandService.increasePenaltyPoint() 안에서 이미 처리됨.
    }
}
