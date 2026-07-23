package com.example.plimap.domain.pin.dto.response;

import com.example.plimap.domain.pin.enums.AvailabilityStatus;
import lombok.Builder;

import java.util.List;

public class PinResponse {
    @Builder
    public record Summary(
            Long pinId,

            Long placeId,

            String writerNickname,

            String writerProfileImage,

            String introduction,

            String previewUrl,

            Integer clipStartMs
    ) {}

    @Builder
    public record PinAvailability(
            AvailabilityStatus status,

            boolean registrable,

            Double distanceFromUserMeters,

            Double nearestPinDistanceMeters
    ) {}

    @Builder
    public record UpdatedPin(
            String introduction,

            List<String> tags,

            Boolean feedOpen
    ) {}

    @Builder
    public record LikeCount(
            Integer likeCount
    ) {}
}
