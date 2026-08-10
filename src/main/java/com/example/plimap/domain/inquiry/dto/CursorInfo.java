package com.example.plimap.domain.inquiry.dto;

import java.time.Instant;

public record CursorInfo(
        Instant createdAt,
        Long id
) {
}
