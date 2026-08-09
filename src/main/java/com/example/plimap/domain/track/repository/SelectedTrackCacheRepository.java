package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import java.util.Optional;

public interface SelectedTrackCacheRepository {

    Optional<SelectedTrackCache> findByItunesTrackId(Long itunesTrackId);

    void save(SelectedTrackCache selectedTrack);
}
