package com.example.plimap.domain.track.enums;

import java.util.Arrays;

public enum YoutubePlaybackFailureType {

    INVALID_PARAMETER(2, false),
    HTML5_PLAYER_ERROR(5, false),
    VIDEO_UNAVAILABLE(100, true),
    EMBED_BLOCKED(101, true),
    UNKNOWN(null, false);

    private final Integer errorCode;
    private final boolean cacheable;

    YoutubePlaybackFailureType(Integer errorCode, boolean cacheable) {
        this.errorCode = errorCode;
        this.cacheable = cacheable;
    }

    public static YoutubePlaybackFailureType from(Integer errorCode) {
        if (Integer.valueOf(150).equals(errorCode)) {
            return EMBED_BLOCKED;
        }
        return Arrays.stream(values())
                .filter(type -> type.errorCode != null)
                .filter(type -> type.errorCode.equals(errorCode))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public boolean isCacheable() {
        return cacheable;
    }
}
