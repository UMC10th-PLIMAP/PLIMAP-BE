package com.example.plimap.domain.pin.dto;

import com.example.plimap.domain.member.enums.MemberStatus;
import java.time.Instant;

public record ReportedPinInfo(
        Long pinId,
        String pinTitle,
        String pinLocation,
        Long authorMemberId,
        String authorNickname,
        MemberStatus authorStatus,
        int reportCount,
        Instant createdAt
) {}
