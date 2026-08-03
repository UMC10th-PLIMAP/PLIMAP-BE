package com.example.plimap.domain.pin.dto.response;

import com.example.plimap.domain.pin.enums.AvailabilityStatus;
import com.example.plimap.domain.pin.enums.ClusterLevel;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

public class PinResponse {
    @Builder
    public record Summary(
            Long pinId,

            Long placeId,

            String writerNickname,

            String writerProfileImage,

            String introduction,

            String  youtubeVideoId,

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

    @Builder
    public record Feed(
            Long pinId,
            String albumImageUrl,
            Double latitude,
            Double longitude,
            String placeName,
            Integer distanceFromUser,
            Long pinCount,
            Instant createdAt
    ) {}

    @Builder
    public record MyPin(
            Long pinId,
            String albumImageUrl,
            String trackTitle,
            String artist,
            String placeName,
            String introduction,
            List<String> tags,
            String staticCreatedAt,
            Instant createdAt
    ) {}

    @Builder
    public record PinDetail(
            Long pinId,
            String writerNickname,
            String writerProfileImage,
            String introduction,
            List<String> tags,
            Integer clipStartMs,
            Integer likeCount,
            Boolean userLike,
            String staticCreatedAt,
            Instant createdAt
    ) {}

    @Builder
    public record PinPreview(
            Long placeId,
            Double latitude,
            Double longitude,
            String writerNickname,
            String writerProfileImage,
            String introduction,
            String albumImageUrl,
            String youtubeVideoId,
            Integer clipStartMs
    ) {}

    @Builder
    public record Cluster(
            ClusterLevel clusterLevel,
            String regionName,
            Double latitude,
            Double longitude,
            Integer pinCount,
            Bound bounds
    ) {}

    @Builder
    public record Bound(
            Double southWestLat,
            Double southWestLng,
            Double northEastLat,
            Double northEastLng
    ) {}

    @Builder
    public record ClusterAndPin(
            Integer zoomLevel,
            List<Cluster> clusters,
            List<PinPreview> pins
    ) {}
}
