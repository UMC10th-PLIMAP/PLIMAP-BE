package com.example.plimap.domain.place.repository.query;

import com.example.plimap.domain.place.dto.NearbyBookmarkedPlace;
import java.util.List;

public interface PlaceBookmarkQueryRepository {

    List<NearbyBookmarkedPlace> findNearbyActiveBookmarks(
            Long memberId,
            double latitude,
            double longitude
    );
}
