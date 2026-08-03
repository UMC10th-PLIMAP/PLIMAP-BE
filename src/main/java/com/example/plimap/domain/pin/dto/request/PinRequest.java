package com.example.plimap.domain.pin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;

public class PinRequest {
    public static final String INVALID_LOCATION_MESSAGE = "위치 정보가 올바르지 않습니다.";

    @Builder
    @Schema(name = "PinCreateRequest")
    public record Create(
            @NotNull(message = "userLatitude는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-90", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "현재 사용자 위치의 위도", example = "37.5297")
            Double userLatitude,

            @NotNull(message = "userLongitude는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-180", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "현재 사용자 위치의 경도", example = "126.9333")
            Double userLongitude,

            @NotNull(message = "placeId는 널이어서는 안 됩니다.")
            @Schema(description = "pin 등록 장소의 id", example = "1")
            Long placeId,

            @NotNull(message = "itunesTrackId는 널이어서는 안 됩니다.")
            @Schema(description = "아이튠즈 노래 id", example = "1764485170")
            Long itunesTrackId,

            @NotNull(message = "clipStartMs은 널이어서는 안 됩니다.")
            @Schema(description = "노래 시작 지점", example = "40000")
            Integer clipStartMs,

            @NotBlank
            @Size(max = 100, message = "소개글은 100자 이하여야 합니다.")
            @Schema(description = "노래 소개글", example = "feeling love attack!")
            String introduction,

            @NotNull(message = "tags는 널이어서는 안 됩니다.")
            @Schema(description = "노래 태그 리스트", example = "[\"몽환\"]")
            List<String> tags,

            @NotNull(message = "feedOpen은 널이어서는 안 됩니다.")
            @Schema(description = "피드 공개 여부", example = "true")
            Boolean feedOpen
    ) {
    }

    @Builder
    public record PinAvailability(
            @NotNull(message = "latitude는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-90", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "선택한 위치의 위도", example = "37.629000")
            Double latitude,

            @NotNull(message = "longitude는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-180", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "선택한 위치의 경도", example = "127.094000")
            Double longitude,

            @NotNull(message = "userLatitude는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-90", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "현재 사용자 위치의 위도", example = "37.626144976334544")
            Double userLatitude,

            @NotNull(message = "userLongitude는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-180", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "현재 사용자 위치의 경도", example = "127.09302024107471")
            Double userLongitude
    ) {}

    @Builder
    public record Update (
            @Pattern(regexp = ".*\\S.*", message = "소개글은 공백일 수 없습니다.")
            @Size(max = 100, message = "소개글은 100자 이하여야 합니다.")
            @Schema(description = "노래 소개글", example = "i am all you need")
            String introduction,

            @Schema(description = "노래 태그 리스트", example = "[\"청량\"]")
            List<String> tags,

            @Schema(description = "피드 공개 여부", example = "false")
            Boolean feedOpen
    ) {}

    @Builder
    public record Viewport (
            @NotNull(message = "southWestLat는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-90", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "현재 화면의 최소 위도 (남서쪽 위도 좌표)", example = "37.626145")
            Double southWestLat,

            @NotNull(message = "southWestLng는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-180", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "현재 화면의 최소 경도 (남서쪽 경도 좌표)", example = "127.093020")
            Double southWestLng,

            @NotNull(message = "northEastLat는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-90", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "현재 화면의 최대 위도 (북동쪽 위도 좌표)", example = "37.629000")
            Double northEastLat,

            @NotNull(message = "northEastLng는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-180", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "현재 화면의 최대 경도 (북동쪽 경도 좌표)", example = "127.094000")
            Double northEastLng,

            @NotNull(message = "zoomLevel은 널이어서는 안 됩니다.")
            @Min(value = 1,  message = "zoomLevel은 1 이상이어야 합니다.")
            @Max(value = 20,  message = "zoomLevel은 20 이하여야 합니다.")
            @Schema(description = "현재 zoom level", example = "7")
            Integer zoomLevel
    ) {

        @AssertTrue(message = "southWestLat는 northEastLat보다 작거나 같아야 합니다.")
        public boolean isLatitudeRangeValid() {
            return southWestLat == null
                    || northEastLat == null
                    || southWestLat <= northEastLat;
        }

        @AssertTrue(message = "southWestLng는 northEastLng보다 작거나 같아야 합니다.")
        public boolean isLongitudeRangeValid() {
            return southWestLng == null
                    || northEastLng == null
                    || southWestLng <= northEastLng;
        }
    }

    @Builder
    public record UserLocation(
            @NotNull(message = "userLatitude는 널이어서는 안 됩니다.")
            @Schema(description = "현재 사용자 위치의 위도", example = "37.5297")
            @DecimalMin(value = "-90", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = INVALID_LOCATION_MESSAGE)
            Double userLatitude,

            @NotNull(message = "userLongitude는 널이어서는 안 됩니다.")
            @DecimalMin(value = "-180", message = INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = INVALID_LOCATION_MESSAGE)
            @Schema(description = "현재 사용자 위치의 경도", example = "126.9333")
            Double userLongitude
    ) {}
}
