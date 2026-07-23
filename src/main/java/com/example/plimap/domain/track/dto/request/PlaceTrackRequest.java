package com.example.plimap.domain.track.dto.request;

import com.example.plimap.domain.track.enums.PlaceTrackSort;

public final class PlaceTrackRequest {

    private PlaceTrackRequest() {
    }

    public record List(
            PlaceTrackSort sort,
            int page,
            int size,
            double latitude,
            double longitude
    ) {
    }
}
