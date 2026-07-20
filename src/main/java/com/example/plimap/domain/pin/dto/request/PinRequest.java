package com.example.plimap.domain.pin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PinRequest {
    public record Create(
            @NotNull(message = "userLatitude는 널이어서는 안 됩니다.")
            @Schema(description = "현재 사용자 위치의 위도", example = "37.5297")
            Double userLatitude,

            @NotNull(message = "userLongitude는 널이어서는 안 됩니다.")
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

    public record PinAvailability(
            @NotNull(message = "latitude는 널이어서는 안 됩니다.")
            @Schema(description = "선택한 위치의 위도", example = "37.629000")
            Double latitude,

            @NotNull(message = "longitude는 널이어서는 안 됩니다.")
            @Schema(description = "선택한 위치의 경도", example = "127.094000")
            Double longitude,

            @NotNull(message = "userLatitude는 널이어서는 안 됩니다.")
            @Schema(description = "현재 사용자 위치의 위도", example = "37.626144976334544")
            Double userLatitude,

            @NotNull(message = "userLongitude는 널이어서는 안 됩니다.")
            @Schema(description = "현재 사용자 위치의 경도", example = "127.09302024107471")
            Double userLongitude
    ) {}
}
