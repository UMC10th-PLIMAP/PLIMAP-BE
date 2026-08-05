package com.example.plimap.domain.admin.service.command;

public interface AdminCommandService {

    void reviewPinReport(Long pinId, boolean grantPenalty);

    void reviewProfileReport(Long memberId, boolean grantPenalty);
}
