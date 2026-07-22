package com.example.plimap.domain.pin.repository.query;

import com.example.plimap.domain.pin.dto.PlacePinInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PinQueryRepository{
    Optional<Double> findNearestActivePinWithin20m(double latitude, double longitude);

    Map<Long, PlacePinInfo> findPinInfosByPlaceIds(List<Long> placeIds);
}
