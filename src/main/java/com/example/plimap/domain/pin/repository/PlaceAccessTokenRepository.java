package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.pin.dto.PlaceAccessToken;

import java.util.Optional;

public interface PlaceAccessTokenRepository {
    void save(PlaceAccessToken placeAccessToken, String token);

    public Optional<PlaceAccessToken> findByToken(String token);
}
