package com.example.plimap.domain.place.controller;

import com.example.plimap.domain.place.controller.docs.PlaceControllerDocs;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.exception.PlaceSuccessCode;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@Validated
public class PlaceController implements PlaceControllerDocs {

    private final PlaceCommandService placeCommandService;
    private final PlaceQueryService placeQueryService;

    @Override
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PlaceResponse.SearchResult>> searchPlaces(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(
                new PlaceRequest.Search(keyword, latitude, longitude)
        );
        return ResponseEntity
                .status(PlaceSuccessCode.PLACE_SEARCH_SUCCESS.getStatus())
                .body(ApiResponse.success(PlaceSuccessCode.PLACE_SEARCH_SUCCESS, result));
    }

    @Override
    @PostMapping("/map-selections")
    public ResponseEntity<ApiResponse<PlaceResponse.MapSelection>> confirmMapSelection(
            @RequestBody PlaceRequest.MapSelection request
    ) {
        PlaceResponse.MapSelection result = placeCommandService.confirmMapSelection(request);
        return ResponseEntity
                .status(PlaceSuccessCode.PLACE_MAP_SELECTION_SUCCESS.getStatus())
                .body(ApiResponse.success(PlaceSuccessCode.PLACE_MAP_SELECTION_SUCCESS, result));
    }
}
