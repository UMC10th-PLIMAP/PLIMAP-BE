package com.example.plimap.domain.track.repository.query;

import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PlaceTrackQueryRepository {

    Slice<PlaceTrackQueryResult> findPlaceTracks(
            Long placeId,
            Long memberId,
            PlaceTrackSort sort,
            Pageable pageable
    );

    boolean existsPlaceBookmark(Long placeId, Long memberId);
}
