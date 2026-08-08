package com.example.plimap.domain.track.dto;

import com.example.plimap.domain.track.enums.YoutubePlaybackFailureType;

public record PlaybackFailureCache(
        Long itunesTrackId,
        String youtubeVideoId,
        Integer errorCode,
        YoutubePlaybackFailureType failureType
) {

    public static PlaybackFailureCache create(
            Long itunesTrackId,
            String youtubeVideoId,
            Integer errorCode,
            YoutubePlaybackFailureType failureType
    ) {
        return new PlaybackFailureCache(
                itunesTrackId,
                youtubeVideoId,
                errorCode,
                failureType
        );
    }
}
