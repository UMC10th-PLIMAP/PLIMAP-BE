package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.pin.dto.PlaceAccessToken;

public interface PlaceAccessTokenRepository {
    void save(PlaceAccessToken placeAccessToken, String token);
}
