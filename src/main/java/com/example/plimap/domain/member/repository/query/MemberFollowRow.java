package com.example.plimap.domain.member.repository.query;

import java.time.Instant;

public record MemberFollowRow(
        Long id,
        String nickname,
        String name,
        String profileImageObjectKey,
        Instant followedAt,
        boolean isFollowing,
        boolean isFollowingViewer
) {
}
