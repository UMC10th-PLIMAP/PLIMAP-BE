package com.example.plimap.domain.track.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TrackRequestTest {

    @Test
    void 검색어의_앞뒤_공백을_제거한다() {
        TrackRequest.Search request = new TrackRequest.Search("  아이유  ", 20);

        assertThat(request.keyword()).isEqualTo("아이유");
    }

    @Test
    void 검색어의_연속_공백을_한_칸으로_축약한다() {
        TrackRequest.Search request = new TrackRequest.Search("아이유   밤편지", 20);

        assertThat(request.keyword()).isEqualTo("아이유 밤편지");
    }

    @Test
    void 검색어의_영문을_소문자로_정규화한다() {
        TrackRequest.Search request = new TrackRequest.Search("  Taylor   SWIFT  ", 20);

        assertThat(request.keyword()).isEqualTo("taylor swift");
    }
}
