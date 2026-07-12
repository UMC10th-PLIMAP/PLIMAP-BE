package com.example.plimap.domain.track.dto.request;

public final class TrackCommand {

    private TrackCommand() {
    }

    public record Create(
            String provider,
            String providerTrackId,
            String title,
            String artistName,
            String albumTitle,
            String albumImageUrl,
            String previewUrl,
            Integer durationMs
    ) {
        public Create {
            requireText(provider, "provider");
            requireText(providerTrackId, "providerTrackId");
            requireText(title, "title");
            requireText(artistName, "artistName");

            if (durationMs != null && durationMs < 0) {
                throw new IllegalArgumentException("durationMs must not be negative");
            }
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
