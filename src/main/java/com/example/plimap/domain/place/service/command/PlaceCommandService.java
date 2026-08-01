package com.example.plimap.domain.place.service.command;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;

public interface PlaceCommandService {

    PlaceResponse.MapSelectionResult confirmMapSelection(PlaceRequest.MapSelection request);

    PlaceResponse.Selection selectSearchPlace(
            Long memberId,
            PlaceRequest.Selection request
    );

    void deleteSearchHistory(Long memberId, Long historyId);
}
