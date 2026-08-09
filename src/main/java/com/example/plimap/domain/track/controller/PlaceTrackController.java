package com.example.plimap.domain.track.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.track.controller.docs.PlaceTrackControllerDocs;
import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import com.example.plimap.domain.track.exception.TrackSuccessCode;
import com.example.plimap.domain.track.service.command.PlaceTrackCommandService;
import com.example.plimap.domain.track.service.query.PlaceTrackQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class PlaceTrackController implements PlaceTrackControllerDocs {

    private final PlaceTrackCommandService placeTrackCommandService;
    private final PlaceTrackQueryService placeTrackQueryService;

    @Override
    @GetMapping("/place-tracks/likes")
    public ResponseEntity<ApiResponse<PlaceTrackResponse.LikedPlaceTrackListResult>>
            getLikedPlaceTracks(
                    @AuthenticationPrincipal AuthMember currentMember,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size
            ) {
        PlaceTrackResponse.LikedPlaceTrackListResult result =
                placeTrackQueryService.getLikedPlaceTracks(
                        currentMember.getMember().getId(),
                        page,
                        size
                );

        return ResponseEntity
                .status(TrackSuccessCode.LIKED_PLACE_TRACK_LIST_SUCCESS.getStatus())
                .body(ApiResponse.success(
                        TrackSuccessCode.LIKED_PLACE_TRACK_LIST_SUCCESS,
                        result
                ));
    }

    @Override
    @GetMapping("/places/{placeId}/tracks")
    public ResponseEntity<ApiResponse<PlaceTrackResponse.PlaceTrackListResult>> getPlaceTracks(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long placeId,
            @RequestParam(defaultValue = "POPULAR") PlaceTrackSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(
                        currentMember.getMember().getId(),
                        placeId,
                        new PlaceTrackRequest.List(
                                sort,
                                page,
                                size,
                                latitude,
                                longitude
                        )
                );

        return ResponseEntity
                .status(TrackSuccessCode.PLACE_TRACK_LIST_SUCCESS.getStatus())
                .body(ApiResponse.success(
                        TrackSuccessCode.PLACE_TRACK_LIST_SUCCESS,
                        result
                ));
    }

    @Override
    @GetMapping("/place-tracks/{placeTrackId}")
    public ResponseEntity<ApiResponse<PlaceTrackResponse.PlaceTrackDetail>>
            getPlaceTrackDetail(
                    @AuthenticationPrincipal AuthMember currentMember,
                    @PathVariable Long placeTrackId,
                    @ModelAttribute PlaceTrackRequest.UserLocation request,
                    @RequestHeader(value = "Place-Access-Token", required = false)
                    String token
            ) {
        PlaceTrackResponse.PlaceTrackDetail result =
                placeTrackQueryService.getPlaceTrackDetail(
                        currentMember.getMember().getId(),
                        placeTrackId,
                        request,
                        token
                );

        return ResponseEntity
                .status(TrackSuccessCode.PLACE_TRACK_DETAIL_SUCCESS.getStatus())
                .body(ApiResponse.success(
                        TrackSuccessCode.PLACE_TRACK_DETAIL_SUCCESS,
                        result
                ));
    }

    @Override
    @PutMapping("/place-tracks/{placeTrackId}/likes")
    public ResponseEntity<ApiResponse<PlaceTrackResponse.PlaceTrackLikeResult>>
            createPlaceTrackLike(
                    @AuthenticationPrincipal AuthMember currentMember,
                    @PathVariable Long placeTrackId
            ) {
        PlaceTrackResponse.PlaceTrackLikeResult result =
                placeTrackCommandService.createPlaceTrackLike(
                        currentMember.getMember().getId(),
                        placeTrackId
                );

        return ResponseEntity
                .status(TrackSuccessCode.PLACE_TRACK_LIKE_PUT_SUCCESS.getStatus())
                .body(ApiResponse.success(
                        TrackSuccessCode.PLACE_TRACK_LIKE_PUT_SUCCESS,
                        result
                ));
    }

    @Override
    @DeleteMapping("/place-tracks/{placeTrackId}/likes")
    public ResponseEntity<ApiResponse<PlaceTrackResponse.PlaceTrackLikeResult>>
            deletePlaceTrackLike(
                    @AuthenticationPrincipal AuthMember currentMember,
                    @PathVariable Long placeTrackId
            ) {
        PlaceTrackResponse.PlaceTrackLikeResult result =
                placeTrackCommandService.deletePlaceTrackLike(
                        currentMember.getMember().getId(),
                        placeTrackId
                );

        return ResponseEntity
                .status(TrackSuccessCode.PLACE_TRACK_LIKE_DELETE_SUCCESS.getStatus())
                .body(ApiResponse.success(
                        TrackSuccessCode.PLACE_TRACK_LIKE_DELETE_SUCCESS,
                        result
                ));
    }
}
