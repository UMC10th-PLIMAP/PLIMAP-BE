package com.example.plimap.domain.member.dto;

import java.time.Instant;

public record MemberSearchCursorInfo(
        Integer followingGroupScore,
        Integer nicknameScore,
        Integer nameScore,
        Instant createdAt,
        Long id
) {
}
