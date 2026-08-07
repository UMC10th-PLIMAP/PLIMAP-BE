package com.example.plimap.domain.pin.dto;

import lombok.Builder;

@Builder
public record PlaceAccessToken(
        Long memberId,
        Long placeId,
        Long sourceFriendPinId
) {
}