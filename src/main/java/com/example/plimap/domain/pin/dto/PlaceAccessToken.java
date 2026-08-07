package com.example.plimap.domain.pin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceAccessToken {

    private Long memberId;
    private Long placeId;
    private Long sourceFriendPinId;
}
