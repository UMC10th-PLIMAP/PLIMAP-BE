package com.example.plimap.domain.pin.dto;

import java.time.Instant;

public record CursorInfo(
        Instant createdAt,
        Long pinId,
        Integer likeCount
) { }
