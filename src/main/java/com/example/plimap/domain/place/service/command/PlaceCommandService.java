package com.example.plimap.domain.place.service.command;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;

public interface PlaceCommandService {

    PlaceResponse.MapSelectionResult confirmMapSelection(PlaceRequest.MapSelection request);

    PlaceResponse.Selection selectSearchPlace(
            Long memberId,
            PlaceRequest.Selection request
    );

    PlaceResponse.BookmarkResult bookmarkPlace(Long memberId, Long placeId);

    PlaceResponse.BookmarkResult deletePlaceBookmark(Long memberId, Long placeId);

    void deleteSearchHistory(Long memberId, Long historyId);
}
