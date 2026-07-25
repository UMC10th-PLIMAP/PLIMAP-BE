package com.example.plimap.domain.member.dto;

import java.time.Instant;

public record CursorInfo(
        Instant createdAt,
        Long memberId
) {
}
