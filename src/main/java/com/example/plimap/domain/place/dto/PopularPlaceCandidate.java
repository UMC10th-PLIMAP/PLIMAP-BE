package com.example.plimap.domain.place.dto;

public record PopularPlaceCandidate(
        Long placeId,
        String placeName,
        double distanceMeters,
        long pinCount
) {
}
