package com.example.plimap.domain.place.service.command;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;

public interface PlaceCommandService {

    PlaceResponse.MapSelection confirmMapSelection(PlaceRequest.MapSelection request);
}
