package com.example.plimap.domain.place.service.query;

import com.example.plimap.domain.place.dto.PlaceAddressDecision;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;

public interface PlaceLocationMetadataService {

    PlaceAdministrativeRegion getAdministrativeRegion(
            double latitude,
            double longitude
    );

    PlaceAddressDecision getAddressDecision(
            double latitude,
            double longitude
    );
}
