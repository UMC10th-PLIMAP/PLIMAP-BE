package com.example.plimap.domain.track.service.query;

import com.example.plimap.domain.track.dto.SelectedTrackCache;

import java.util.Optional;

public interface SelectedTrackCacheReader {

    Optional<SelectedTrackCache> findByItunesTrackId(Long itunesTrackId);
}
