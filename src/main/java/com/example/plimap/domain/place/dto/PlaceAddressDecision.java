package com.example.plimap.domain.place.dto;

public record PlaceAddressDecision(
        String buildingName,
        String address,
        String roadAddress
) {

    public boolean hasBuildingName() {
        return buildingName != null;
    }
}
