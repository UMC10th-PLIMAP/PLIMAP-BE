package com.example.plimap.domain.report.service.command;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.report.dto.request.ReportRequest;
import java.util.List;

public interface ReportCommandService {

    void reportMember(Member reporter, Long reportedMemberId, ReportRequest.Create request);

    void reportPin(Member reporter, Long reportedPinId, ReportRequest.Create request);

    void deleteReportsByPinIds(List<Long> pinIds);

    void deleteReportsAgainstMember(Long memberId);

    void deleteReportsByReporter(Long memberId);
}