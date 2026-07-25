package com.example.plimap.domain.track.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.track.controller.docs.PlaceTrackDetailControllerDocs;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.exception.TrackSuccessCode;
import com.example.plimap.domain.track.service.query.PlaceTrackQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/place-tracks")
@RequiredArgsConstructor
@Validated
public class PlaceTrackDetailController implements PlaceTrackDetailControllerDocs {

    private final PlaceTrackQueryService placeTrackQueryService;

    @Override
    @GetMapping("/{placeTrackId}")
    public ResponseEntity<ApiResponse<PlaceTrackResponse.PlaceTrackDetail>>
            getPlaceTrackDetail(
                    @AuthenticationPrincipal AuthMember currentMember,
                    @PathVariable Long placeTrackId
            ) {
        PlaceTrackResponse.PlaceTrackDetail result =
                placeTrackQueryService.getPlaceTrackDetail(
                        currentMember.getMember().getId(),
                        placeTrackId
                );

        return ResponseEntity
                .status(TrackSuccessCode.PLACE_TRACK_DETAIL_SUCCESS.getStatus())
                .body(ApiResponse.success(
                        TrackSuccessCode.PLACE_TRACK_DETAIL_SUCCESS,
                        result
                ));
    }
}
