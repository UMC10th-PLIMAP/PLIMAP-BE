package com.example.plimap.domain.pin.dto.response;

import lombok.Builder;

public class PinResponse {
    @Builder
    public record Summary(
            Long pinId,

            String writerNickname,

            String writerProfileImage,

            String introduction,

            String previewUrl,

            Integer clipStartMs
    ) {}
}
