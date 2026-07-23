package com.example.plimap.domain.place.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PlaceRequest {

    private static final String INVALID_LOCATION_MESSAGE = "위치 정보가 올바르지 않습니다.";

    private PlaceRequest() {
    }

    public record Search(
            String keyword,
            Double latitude,
            Double longitude
    ) {

        public Search {
            keyword = normalize(keyword);
        }

        private static String normalize(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return value.strip();
        }
    }

    public record MapSelection(
            @NotNull(message = INVALID_LOCATION_MESSAGE)
            @DecimalMin(value = "-90", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "선택한 위치의 위도", example = "37.5283")
            Double latitude,

            @NotNull(message = INVALID_LOCATION_MESSAGE)
            @DecimalMin(value = "-180", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "선택한 위치의 경도", example = "126.9326")
            Double longitude,

            @Size(max = 100, message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "장소명", example = "물빛무대 앞 광장")
            String placeName,

            @NotBlank(message = INVALID_LOCATION_MESSAGE)
            @Size(max = 255, message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "지번 주소", example = "서울특별시 영등포구 여의도동")
            String address,

            @Size(max = 255, message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "도로명 주소", example = "서울특별시 영등포구 여의동로")
            String roadAddress
    ) {

        public MapSelection {
            placeName = normalize(placeName);
            address = normalize(address);
            roadAddress = normalize(roadAddress);
        }

        @JsonIgnore
        @Schema(hidden = true)
        @AssertTrue(message = INVALID_LOCATION_MESSAGE)
        public boolean isResolvedPlaceNameValid() {
            String resolvedName = placeName != null
                    ? placeName
                    : roadAddress != null ? roadAddress : address;
            return resolvedName == null || resolvedName.length() <= 100;
        }

        private static String normalize(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return value.strip();
        }
    }
}
