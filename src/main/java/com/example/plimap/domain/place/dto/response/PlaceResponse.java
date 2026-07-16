package com.example.plimap.domain.place.dto.response;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import io.swagger.v3.oas.annotations.media.Schema;

public final class PlaceResponse {

    private PlaceResponse() {
    }

    public record MapSelection(
            @Schema(description = "장소 ID", example = "12")
            Long placeId,
            @Schema(description = "장소명", example = "물빛무대 앞 광장")
            String placeName,
            @Schema(description = "장소 생성 출처", example = "MAP_SELECTION")
            PlaceSource source,
            @Schema(description = "장소 위도", example = "37.5283")
            Double latitude,
            @Schema(description = "장소 경도", example = "126.9326")
            Double longitude
    ) {

        public static MapSelection from(Place place) {
            return new MapSelection(
                    place.getId(),
                    place.getName(),
                    place.getSource(),
                    place.getLocation().getY(),
                    place.getLocation().getX()
            );
        }
    }
}
