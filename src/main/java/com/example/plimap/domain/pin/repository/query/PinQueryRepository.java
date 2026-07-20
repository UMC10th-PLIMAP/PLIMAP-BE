package com.example.plimap.domain.pin.repository.query;

import java.util.Optional;

public interface PinQueryRepository{
    Optional<Double> findNearestActivePinWithin20m(double latitude, double longitude);
}
