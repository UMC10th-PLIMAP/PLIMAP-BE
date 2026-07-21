package com.example.plimap.domain.track.dto.request;

import lombok.Builder;

public final class TrackCommand {

    private TrackCommand() {
    }

    @Builder
    public record Create(
            Long itunesTrackId
    ) {
        public Create {
            if (itunesTrackId == null) {
                throw new IllegalArgumentException("itunesTrackId must not be null");
            }
            if (itunesTrackId <= 0) {
                throw new IllegalArgumentException("itunesTrackId must be positive");
            }
        }
    }
}
