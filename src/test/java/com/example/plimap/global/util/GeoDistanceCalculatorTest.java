package com.example.plimap.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeoDistanceCalculatorTest {

    @Test
    void 같은_좌표의_거리는_0미터다() {
        double distance = GeoDistanceCalculator.calculateMeters(
                37.5283,
                126.9326,
                37.5283,
                126.9326
        );

        assertThat(distance).isZero();
    }

    @Test
    void 두_좌표_사이의_거리를_미터로_계산한다() {
        double distance = GeoDistanceCalculator.calculateMeters(
                37.5283,
                126.9326,
                37.5293,
                126.9326
        );

        assertThat(distance).isBetween(111.0, 112.0);
    }
}
