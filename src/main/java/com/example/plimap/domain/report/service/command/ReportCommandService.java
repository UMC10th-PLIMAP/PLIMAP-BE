package com.example.plimap.domain.report.service.command;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.report.dto.request.ReportRequest;

public interface ReportCommandService {

    void reportMember(Member reporter, Long reportedMemberId, ReportRequest.Create request);

    void reportPin(Member reporter, Long reportedPinId, ReportRequest.Create request);
}