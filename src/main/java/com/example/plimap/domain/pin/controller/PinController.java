package com.example.plimap.domain.pin.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.pin.controller.docs.PinControllerDocs;
import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.enums.PinSortType;
import com.example.plimap.domain.pin.exception.PinSuccessCode;
import com.example.plimap.domain.pin.service.command.impl.PinCommandServiceImpl;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Pin", description = "Pin 관련 api")
public class PinController implements PinControllerDocs {

    private final PinCommandServiceImpl pinCommandService;
    private final PinQueryService pinQueryService;

    @PostMapping("/pins")
    public ResponseEntity<ApiResponse<PinResponse.Summary>> createPin(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestBody @Valid PinRequest.Create request
    ) {
        PinResponse.Summary response = pinCommandService.createPin(currentMember.getMember(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(PinSuccessCode.PIN_CREATE_SUCCESS, response));
    }

    @PostMapping("/pins/availability")
    public ResponseEntity<ApiResponse<PinResponse.PinAvailability>> validatePinAvailability(
            @RequestBody @Valid PinRequest.PinAvailability request
    ) {
        PinResponse.PinAvailability response = pinQueryService.validatePinAvailability(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.PIN_AVAILABILITY_CHECK_SUCCESS, response));
    }

    @PatchMapping("/pins/{pinId}")
    public ResponseEntity<ApiResponse<PinResponse.UpdatedPin>> updatePin(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestBody @Valid PinRequest.Update request,
            @PathVariable Long pinId
    ) {
        PinResponse.UpdatedPin response = pinCommandService.updatePin(currentMember.getMember(), request, pinId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.PIN_UPDATE_SUCCESS, response));
    }

    @DeleteMapping("/pins/{pinId}")
    public ResponseEntity<ApiResponse<Void>> deletePin(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId
    ) {
        pinCommandService.deletePin(currentMember.getMember(),pinId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.PIN_DELETE_SUCCESS, null));
    }

    @PutMapping("/pins/{pinId}/likes")
    public ResponseEntity<ApiResponse<PinResponse.LikeCount>> createPinLike(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId
    ) {
        PinResponse.LikeCount response = pinCommandService.createPinLike(currentMember.getMember(), pinId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.PIN_LIKE_PUT_SUCCESS, response));
    }

    @DeleteMapping("/pins/{pinId}/likes")
    public ResponseEntity<ApiResponse<PinResponse.LikeCount>> deletePinLike(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId
    ) {
        PinResponse.LikeCount response = pinCommandService.deletePinLike(currentMember.getMember(), pinId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.PIN_LIKE_DELETE_SUCCESS, response));
    }

    @GetMapping("/feed/members/me")
    public ResponseEntity<ApiResponse<Pagination<PinResponse.Feed>>> getMyFeedList(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        Pagination<PinResponse.Feed> response = pinQueryService.findFeedListByMemberId(
                currentMember.getMember().getId(), currentMember.getMember().getId(), cursor, pageSize);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.MY_FEED_LIST_SEARCH_SUCCESS, response));
    }

    @GetMapping("/feed/members/{memberId}")
    public ResponseEntity<ApiResponse<Pagination<PinResponse.Feed>>> getMemberFeedList(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long memberId,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        // 이 경로는 SecurityConfig에서 인증 없이도 허용되므로 currentMember가 null일 수 있다.
        Long viewerId = currentMember != null ? currentMember.getMember().getId() : null;
        Pagination<PinResponse.Feed> response = pinQueryService.findFeedListByMemberId(memberId, viewerId, cursor, pageSize);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.MEMBER_FEED_LIST_SEARCH_SUCCESS, response));
    }

    @GetMapping("/pins/members/me")
    public ResponseEntity<ApiResponse<Pagination<PinResponse.MyPin>>> getMyPinList(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        Pagination<PinResponse.MyPin> response = pinQueryService.findMyPinList(currentMember.getMember().getId(), cursor, pageSize);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.MY_PIN_LIST_SEARCH_SUCCESS, response));
    }

    @GetMapping("/place-tracks/{placeTrackId}/pins")
    public ResponseEntity<ApiResponse<Pagination<PinResponse.PinDetail>>> getPlaceTrackPinList(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "LATEST")
            PinSortType pinSortType,
            @PathVariable Long placeTrackId
    ) {
        Pagination<PinResponse.PinDetail> response = pinQueryService.findPinListByPlaceTrackIdAndSortType(currentMember.getMember().getId(), cursor, pageSize, pinSortType, placeTrackId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.PLACE_TRACK_PIN_LIST_SEARCH_SUCCESS, response));
    }

    @GetMapping("/pins/{pinId}")
    public ResponseEntity<ApiResponse<PinResponse.PinPreview>> getPinPreview(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId
    ) {
        PinResponse.PinPreview response = pinQueryService.getPinPreview(pinId, currentMember.getMember().getId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(PinSuccessCode.PIN_SEARCH_SUCCESS, response));
    }
}
