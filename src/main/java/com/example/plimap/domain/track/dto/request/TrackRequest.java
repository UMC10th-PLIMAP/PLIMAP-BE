package com.example.plimap.domain.track.dto.request;

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
}
