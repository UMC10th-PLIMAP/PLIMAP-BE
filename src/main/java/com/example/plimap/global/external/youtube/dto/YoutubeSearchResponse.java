package com.example.plimap.global.external.youtube.dto;

import java.util.List;

public record YoutubeSearchResponse(List<Item> items) {

    public YoutubeSearchResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Item(Id id) {
    }

    public record Id(String videoId) {
    }
}
