package com.example.plimap.domain.report.dto;

import com.example.plimap.domain.report.enums.ReportCategory;
import java.time.Instant;

public record ReportReason(
        Long pinId,
        ReportCategory category,
        String detail,
        String reporterNickname,
        Instant createdAt
) {}
