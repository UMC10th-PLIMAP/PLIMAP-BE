package com.example.plimap.global.util;

public final class GeoDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoDistanceCalculator() {
    }

    public static double calculateMeters(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2)
                * Math.sin(longitudeDelta / 2);
        haversine = Math.min(1.0, Math.max(0.0, haversine));
        double angularDistance = 2 * Math.atan2(
                Math.sqrt(haversine),
                Math.sqrt(1 - haversine)
        );
        return EARTH_RADIUS_METERS * angularDistance;
    }
}
