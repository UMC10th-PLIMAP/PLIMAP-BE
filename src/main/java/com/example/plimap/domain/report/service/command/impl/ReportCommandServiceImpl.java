package com.example.plimap.domain.report.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.report.dto.request.ReportRequest;
import com.example.plimap.domain.report.entity.Report;
import com.example.plimap.domain.report.exception.ReportErrorCode;
import com.example.plimap.domain.report.exception.ReportException;
import com.example.plimap.domain.report.repository.ReportRepository;
import com.example.plimap.domain.report.service.command.ReportCommandService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommandServiceImpl implements ReportCommandService {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;
    private final PinQueryService pinQueryService;
    private final PinCommandService pinCommandService;
    private final ReportRepository reportRepository;

    @Override
    public void reportMember(Member reporter, Long reportedMemberId, ReportRequest.Create request) {
        Member reportedMember = memberQueryService.getActiveMember(reportedMemberId);
        Report report = Report.createMemberReport(
                reporter,
                reportedMember,
                request.category(),
                request.detail()
        );

        if (reportRepository.existsByReporter_IdAndReportedMember_Id(
                reporter.getId(),
                reportedMemberId
        )) {
            throw new ReportException(ReportErrorCode.REPORT_MEMBER_ALREADY_EXISTS);
        }

        saveReport(report, ReportErrorCode.REPORT_MEMBER_ALREADY_EXISTS);
        memberCommandService.increaseReportCount(reportedMemberId);
    }

    @Override
    public void reportPin(Member reporter, Long reportedPinId, ReportRequest.Create request) {
        Pin reportedPin = pinQueryService.getActivePin(reportedPinId);
        validatePinReportTarget(reporter, reportedPin);

        Report report = Report.createPinReport(
                reporter,
                reportedPin,
                request.category(),
                request.detail()
        );

        if (reportRepository.existsByReporter_IdAndReportedPin_Id(reporter.getId(), reportedPinId)) {
            throw new ReportException(ReportErrorCode.REPORT_PIN_ALREADY_EXISTS);
        }

        saveReport(report, ReportErrorCode.REPORT_PIN_ALREADY_EXISTS);
        pinCommandService.increaseReportCount(reportedPinId);
    }

    private void validatePinReportTarget(Member reporter, Pin reportedPin) {
        if (isSameMember(reporter, reportedPin.getMember())) {
            throw new ReportException(ReportErrorCode.REPORT_OWN_PIN_NOT_ALLOWED);
        }
        if (!reportedPin.isFeedPublic()) {
            throw new ReportException(ReportErrorCode.REPORT_PRIVATE_PIN_NOT_ALLOWED);
        }
    }

    private boolean isSameMember(Member reporter, Member pinAuthor) {
        if (reporter == pinAuthor) {
            return true;
        }
        return reporter.getId() != null && reporter.getId().equals(pinAuthor.getId());
    }

    @Override
    public void deleteReportsByPinIds(List<Long> pinIds) {
        if (pinIds.isEmpty()) {
            return;
        }
        reportRepository.deleteAllByReportedPinIdIn(pinIds);
    }

    @Override
    public void deleteReportsAgainstMember(Long memberId) {
        reportRepository.deleteAllByReportedMemberId(memberId);
    }

    @Override
    public void deleteReportsByReporter(Long memberId) {
        // 삭제될 신고가 대상(핀/회원)의 reportCount에 이미 반영돼 있으므로, row를 지우기 전에
        // 캐시된 카운트를 먼저 보정해야 신고 이력과 자동숨김 기준(reportCount)이 어긋나지 않는다.
        reportRepository.findReportedPinIdsByReporterId(memberId).forEach(pinCommandService::decreaseReportCount);
        reportRepository.findReportedMemberIdsByReporterId(memberId).forEach(memberCommandService::decreaseReportCount);
        reportRepository.deleteAllByReporterId(memberId);
    }

    @Override
    public void markPinReportsReviewed(Long pinId) {
        reportRepository.markReviewedByReportedPinId(pinId);
    }

    private void saveReport(Report report, ReportErrorCode duplicateErrorCode) {
        try {
            reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException exception) {
            // 동시 요청이 사전 중복 검사를 통과해도 DB 유니크 인덱스에서 최종 차단한다.
            throw new ReportException(duplicateErrorCode, exception);
        }
    }
}