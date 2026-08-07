package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.dto.PlaybackFailureCache;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface PlaybackFailureCacheRepository {

    Optional<PlaybackFailureCache> findByItunesTrackId(Long itunesTrackId);

    Map<Long, PlaybackFailureCache> findAllByItunesTrackIds(Collection<Long> itunesTrackIds);

    void save(PlaybackFailureCache playbackFailure);
}
