package com.example.plimap.domain.pin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PinRequest {
    public record Create(
            @NotNull(message = "userLatitude는 널이어서는 안 됩니다.")
            Double userLatitude,

            @NotNull(message = "userLongitude는 널이어서는 안 됩니다.")
            Double userLongitude,

            @NotNull(message = "placeId는 널이어서는 안 됩니다.")
            Long placeId,

            @NotNull(message = "itunesTrackId는 널이어서는 안 됩니다.")
            Long itunesTrackId,

            @NotNull(message = "clipStartMs은 널이어서는 안 됩니다.")
            Integer clipStartMs,

            @NotBlank
            @Size(max = 100, message = "소개글은 100자 이하여야 합니다.")
            String introduction,

            @NotNull(message = "tags는 널이어서는 안 됩니다.")
            List<String> tags,

            @NotNull(message = "feedOpen은 널이어서는 안 됩니다.")
            Boolean feedOpen
    ) {
    }
}
