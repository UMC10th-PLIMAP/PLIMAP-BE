package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.dto.TrackMetadataCache;
import java.util.Optional;

public interface TrackMetadataCacheRepository {

    Optional<TrackMetadataCache> findByItunesTrackId(Long itunesTrackId);

    void save(TrackMetadataCache metadata);
}
