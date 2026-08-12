package com.example.plimap.domain.notification.dto.response;

import com.example.plimap.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public class NotificationResponse {

    public record Item(
            Long notificationId,

            @Schema(example = "PIN_LIKED")
            NotificationType type,

            Long actorId,

            @Schema(description = "알림을 발생시킨 회원의 닉네임")
            String actorNickname,

            String actorProfileImageUrl,

            @Schema(description = "FOLLOW 유형에서 내가 actor를 팔로우 중인지")
            boolean isFollowing,

            @Schema(description = "FOLLOW 유형에서 actor가 나를 팔로우 중인지")
            boolean isFollowingViewer,

            @Schema(description = "PIN_CREATED, PIN_LIKED 유형일 때만 존재")
            Long pinId,

            @Schema(description = "PIN_CREATED, PIN_LIKED 유형일 때만 존재")
            String placeName,

            @Schema(description = "PIN_CREATED, PIN_LIKED 유형일 때만 존재")
            String albumImageUrl,

            @Schema(example = "false")
            boolean read,

            Instant createdAt
    ) {}
}
