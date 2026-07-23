package com.example.plimap.domain.report.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.report.dto.request.ReportRequest;
import com.example.plimap.domain.report.entity.Report;
import com.example.plimap.domain.report.exception.ReportErrorCode;
import com.example.plimap.domain.report.exception.ReportException;
import com.example.plimap.domain.report.repository.ReportRepository;
import com.example.plimap.domain.report.service.command.ReportCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommandServiceImpl implements ReportCommandService {

    private final MemberQueryService memberQueryService;
    private final PinQueryService pinQueryService;
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

    private void saveReport(Report report, ReportErrorCode duplicateErrorCode) {
        try {
            reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException exception) {
            // 동시 요청이 사전 중복 검사를 통과해도 DB 유니크 인덱스에서 최종 차단한다.
            throw new ReportException(duplicateErrorCode, exception);
        }
    }
}