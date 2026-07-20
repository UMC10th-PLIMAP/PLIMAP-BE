package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.dto.TrackSearchCache;
import java.util.Optional;

public interface TrackSearchCacheRepository {

    Optional<TrackSearchCache> find(String normalizedKeyword, int limit);

    void save(String normalizedKeyword, int limit, TrackSearchCache searchResult);
}
