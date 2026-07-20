package com.example.plimap.domain.track.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Locale;

public final class TrackRequest {

    private TrackRequest() {
    }

    public record Search(String keyword, int limit) {

        private static final String CONSECUTIVE_WHITESPACE = "\\s+";

        public Search {
            keyword = normalize(keyword);
        }

        private static String normalize(String keyword) {
            if (keyword == null) {
                return null;
            }
            return keyword.trim()
                    .replaceAll(CONSECUTIVE_WHITESPACE, " ")
                    .toLowerCase(Locale.ROOT);
        }
    }

    public record PlaybackPreparation(
            @NotNull(message = "iTunes 트랙 ID를 입력해주세요.")
            @Positive(message = "iTunes 트랙 ID는 양수여야 합니다.")
            Long itunesTrackId
    ) {
    }
}
