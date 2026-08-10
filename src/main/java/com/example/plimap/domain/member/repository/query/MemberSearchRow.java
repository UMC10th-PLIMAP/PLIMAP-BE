package com.example.plimap.domain.member.repository.query;

import java.time.Instant;

public record MemberSearchRow(
        Long id,
        String nickname,
        String name,
        String profileImageObjectKey,
        Instant createdAt,
        boolean isFollowing,
        boolean isFollowingViewer,
        int nicknameScore,
        int nameScore
) {
}
