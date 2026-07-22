package com.example.plimap.domain.pin.dto;

import lombok.Builder;

@Builder
public record PlacePinInfo(
        boolean hasPin,
        String firstPinCreatorNickname
) {}