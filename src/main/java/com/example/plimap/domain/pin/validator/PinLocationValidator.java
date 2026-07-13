package com.example.plimap.domain.pin.validator;

import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.place.entity.Place;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

@Component
public class PinLocationValidator {

    private static final double EARTH_RADIUS = 6371000;

    public void validateWithin500m(
            double currentLat,
            double currentLng,
            Place place
    ) {

        Point point = place.getLocation();

        double placeLat = point.getY();
        double placeLng = point.getX();

        double distance = calculateDistance(
                currentLat, currentLng,
                placeLat, placeLng
        );

        if (distance > 500) {
            throw new PinException(PinErrorCode.LOCATION_DISTANCE_INVALID);
        }
    }

    private double calculateDistance(
            double lat1, double lng1,
            double lat2, double lng2
    ) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2)
                        * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}