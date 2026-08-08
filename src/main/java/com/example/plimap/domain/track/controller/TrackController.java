package com.example.plimap.domain.track.controller;

import com.example.plimap.domain.track.controller.docs.TrackControllerDocs;
import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.dto.response.TrackResponse;
import com.example.plimap.domain.track.exception.TrackSuccessCode;
import com.example.plimap.domain.track.service.command.TrackPlaybackFailureService;
import com.example.plimap.domain.track.service.command.TrackPlaybackPreparationService;
import com.example.plimap.domain.track.service.query.TrackQueryService;
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
@RequestMapping("/api/v1/tracks")
@RequiredArgsConstructor
@Validated
public class TrackController implements TrackControllerDocs {

    private final TrackQueryService trackQueryService;
    private final TrackPlaybackPreparationService trackPlaybackPreparationService;
    private final TrackPlaybackFailureService trackPlaybackFailureService;

    @Override
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<TrackResponse.TrackSearchResult>> searchTracks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit
    ) {
        TrackResponse.TrackSearchResult result =
                trackQueryService.searchTracks(new TrackRequest.Search(keyword, limit));
        return ResponseEntity
                .status(TrackSuccessCode.TRACK_SEARCH_SUCCESS.getStatus())
                .body(ApiResponse.success(TrackSuccessCode.TRACK_SEARCH_SUCCESS, result));
    }

    @Override
    @PostMapping("/playback-preparations")
    public ResponseEntity<ApiResponse<TrackResponse.PlaybackPreparationResult>> preparePlayback(
            @RequestBody TrackRequest.PlaybackPreparation request
    ) {
        TrackResponse.PlaybackPreparationResult result =
                trackPlaybackPreparationService.prepare(request);
        return ResponseEntity
                .status(TrackSuccessCode.PLAYBACK_PREPARATION_SUCCESS.getStatus())
                .body(ApiResponse.success(
                        TrackSuccessCode.PLAYBACK_PREPARATION_SUCCESS,
                        result
                ));
    }

    @Override
    @PostMapping("/playback-failures")
    public ResponseEntity<ApiResponse<Void>> reportPlaybackFailure(
            @RequestBody TrackRequest.PlaybackFailure request
    ) {
        trackPlaybackFailureService.report(request);
        return ResponseEntity
                .status(TrackSuccessCode.PLAYBACK_FAILURE_REPORTED.getStatus())
                .body(ApiResponse.success(
                        TrackSuccessCode.PLAYBACK_FAILURE_REPORTED,
                        null
                ));
    }
}
