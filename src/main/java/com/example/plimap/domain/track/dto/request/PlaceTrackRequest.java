package com.example.plimap.domain.track.dto.request;

import com.example.plimap.domain.track.enums.PlaceTrackSort;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public final class PlaceTrackRequest {

    private PlaceTrackRequest() {
    }

    public record List(
            PlaceTrackSort sort,
            int page,
            int size,
            @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
            @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
            double latitude,
            @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
            @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
            double longitude
    ) {
    }
}
