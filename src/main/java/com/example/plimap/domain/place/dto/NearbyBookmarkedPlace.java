package com.example.plimap.domain.place.dto;

public record NearbyBookmarkedPlace(
        Long placeId,
        String placeName,
        Integer distanceMeters
) {
}
