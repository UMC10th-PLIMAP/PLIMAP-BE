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

    public static final String INVALID_LOCATION_MESSAGE = "위치 정보가 올바르지 않습니다.";

    private PlaceRequest() {
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static String preserveOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record Search(
            String keyword,
            @DecimalMin(value = "-90", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = INVALID_LOCATION_MESSAGE)
            Double latitude,
            @DecimalMin(value = "-180", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = INVALID_LOCATION_MESSAGE)
            Double longitude
    ) {

        public Search {
            keyword = normalize(keyword);
        }
    }

    @Schema(name = "PlaceSelectionRequest")
    public record Selection(
            @Schema(
                    description = "검색 결과 유형",
                    example = "PLACE",
                    allowableValues = {"PLACE", "ADDRESS"}
            )
            String resultType,

            @Schema(description = "장소 검색 provider", example = "KAKAO")
            String provider,

            @Schema(
                    description = "provider가 제공하는 장소 ID. ADDRESS이면 null",
                    example = "26338954",
                    nullable = true
            )
            String providerPlaceId,

            @Schema(
                    description = "장소명. ADDRESS이면 roadAddress, address 순으로 서버에서 결정",
                    example = "한강"
            )
            String placeName,

            @Schema(
                    description = "장소 카테고리. ADDRESS이면 null",
                    example = "공원",
                    nullable = true
            )
            String category,

            @Schema(description = "지번 주소", example = "서울특별시 영등포구 여의도동")
            String address,

            @Schema(description = "도로명 주소", example = "서울특별시 영등포구 여의동로")
            String roadAddress,

            @Schema(description = "장소 위도", example = "37.5283")
            Double latitude,

            @Schema(description = "장소 경도", example = "126.9326")
            Double longitude,

            @Schema(description = "사용자 현재 위도", example = "37.5251")
            Double userLatitude,

            @Schema(description = "사용자 현재 경도", example = "126.9298")
            Double userLongitude
    ) {

        public Selection {
            resultType = normalize(resultType);
            provider = normalize(provider);
            placeName = normalize(placeName);
            if ("ADDRESS".equals(resultType)) {
                address = preserveOrNull(address);
                roadAddress = preserveOrNull(roadAddress);
            } else {
                providerPlaceId = normalize(providerPlaceId);
                category = normalize(category);
                address = normalize(address);
                roadAddress = normalize(roadAddress);
            }
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

    }
}
