package com.example.plimap.domain.track.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SelectedTrackCacheTest {

    @Test
    void metadata와_YouTube_videoId를_모든_selection_필드에_매핑한다() {
        TrackMetadataCache metadata = new TrackMetadataCache(
                123L,
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );

        SelectedTrackCache result = SelectedTrackCache.from(metadata, "abcdefghijk");

        assertThat(result).isEqualTo(new SelectedTrackCache(
                123L,
                "abcdefghijk",
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        ));
    }
}
