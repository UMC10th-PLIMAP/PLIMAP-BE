package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    Optional<Track> findByProviderAndProviderTrackId(String provider, String providerTrackId);
}
