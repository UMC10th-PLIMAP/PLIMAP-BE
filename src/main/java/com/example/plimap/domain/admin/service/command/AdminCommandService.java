package com.example.plimap.domain.admin.service.command;

import com.example.plimap.domain.admin.dto.response.AdminResponse;
import com.example.plimap.domain.member.enums.SuspensionPeriod;
import com.example.plimap.domain.report.enums.ReportCategory;

public interface AdminCommandService {

    void reviewPinReport(Long pinId, boolean grantPenalty);

    void reviewProfileReport(Long memberId, boolean grantPenalty);

    void grantPinSanction(Long pinId, Long reportId, SuspensionPeriod period);

    void grantMemberSanction(Long memberId, SuspensionPeriod period, ReportCategory reasonCategory, String reasonDetail);

    AdminResponse.MemberDetail regenerateMemberNickname(Long memberId);
}
