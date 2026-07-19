package com.example.plimap.domain.track.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.global.external.itunes.dto.ItunesSearchResponse;
import org.junit.jupiter.api.Test;

class TrackResponseTest {

    @Test
    void iTunes_results가_null이면_빈_검색_결과로_변환한다() {
        ItunesSearchResponse response = new ItunesSearchResponse(0, null);

        TrackResponse.SearchResult result = TrackResponse.SearchResult.from(response);

        assertThat(result.tracks()).isEmpty();
    }
}
