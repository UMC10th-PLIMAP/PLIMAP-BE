package com.example.plimap.domain.admin.service.command;

import com.example.plimap.domain.admin.dto.response.AdminResDTO;

public interface AdminCommandService {

    void reviewPinReport(Long pinId, boolean grantPenalty);

    void reviewProfileReport(Long memberId, boolean grantPenalty);

    AdminResDTO.MemberDetail regenerateMemberNickname(Long memberId);
}
