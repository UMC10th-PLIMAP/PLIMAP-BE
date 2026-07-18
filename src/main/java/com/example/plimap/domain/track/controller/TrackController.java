package com.example.plimap.domain.track.controller;

import com.example.plimap.domain.track.controller.docs.TrackControllerDocs;
import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.dto.response.TrackResponse;
import com.example.plimap.domain.track.exception.TrackSuccessCode;
import com.example.plimap.domain.track.service.query.TrackQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/tracks")
@RequiredArgsConstructor
@Validated
public class TrackController implements TrackControllerDocs {

    private final TrackQueryService trackQueryService;

    @Override
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<TrackResponse.SearchResult>> searchTracks(
            @RequestParam
            String keyword,
            @RequestParam(defaultValue = "20")
            int limit
    ) {
        TrackResponse.SearchResult result =
                trackQueryService.searchTracks(new TrackRequest.Search(keyword, limit));
        return ResponseEntity
                .status(TrackSuccessCode.TRACK_SEARCH_SUCCESS.getStatus())
                .body(ApiResponse.success(TrackSuccessCode.TRACK_SEARCH_SUCCESS, result));
    }
}
