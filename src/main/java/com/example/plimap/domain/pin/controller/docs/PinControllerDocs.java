package com.example.plimap.domain.pin.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface PinControllerDocs {

    @Operation(
            summary = "PIN 등록",
            description = "해당 pin, 장소, 노래 정보를 저장합니다.(Figma 기준 화면: PN-03-02)"
    )
    public ResponseEntity<ApiResponse<PinResponse.Summary>> createPin(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestBody @Valid PinRequest.Create request
    );

    @Operation(
            summary = "지도 선택 위치 검증",
            description = "해당 위치에 핀을 등록할 수 있는지 검증합니다. (Figma 기준 화면: PN-02-02)"
    )
    public ResponseEntity<ApiResponse<PinResponse.PinAvailability>> validatePinAvailability(
            @RequestBody @Valid PinRequest.PinAvailability request
    ) ;

    @Operation(
            summary = "PIN 수정",
            description = "핀 내용을 수정합니다. (Figma 기준 화면: 추후 추가 예정)"
    )
    public ResponseEntity<ApiResponse<PinResponse.UpdatedPin>> updatePin(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestBody @Valid PinRequest.Update request,
            @PathVariable Long pinId
    );

    @Operation(
            summary = "PIN 삭제",
            description = "핀 내용을 삭제합니다. (Figma 기준 화면: 추후 추가 예정)"
    )
    public ResponseEntity<ApiResponse<Void>> deletePin(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId
    );

    @Operation(
            summary = "PIN 좋아요 등록",
            description = "사용자의 핀 좋아요(따봉)을 등록합니다. (Figma 기준 화면: PN-01-03)"
    )
    public ResponseEntity<ApiResponse<PinResponse.LikeCount>> createPinLike(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId
    );

    @Operation(
            summary = "PIN 좋아요 삭제",
            description = "사용자의 핀 좋아요(따봉)을 삭제합니다. (Figma 기준 화면: PN-01-03)"
    )
    public ResponseEntity<ApiResponse<PinResponse.LikeCount>> deletePinLike(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId
    );

    @Operation(
            summary = "내 피드 목록 조회",
            description = "내 피드 목록을 조회합니다. (Figma 기준 화면: FD-01-01)"
    )
    public ResponseEntity<ApiResponse<Pagination<PinResponse.Feed>>> getMyFeedList(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor
    );

    @Operation(
            summary = "타인 피드 목록 조회",
            description = "타인 피드 목록을 조회합니다. (Figma 기준 화면: FD-02-01)"
    )
    public ResponseEntity<ApiResponse<Pagination<PinResponse.Feed>>> getMemberFeedList(
            @PathVariable Long memberId,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor
    );

    @Operation(
            summary = "내가 작성한 PIN 목록 조회",
            description = "내가 작성한 PIN 목록 조회합니다. (Figma 기준 화면: FD-01-03-b2)"
    )
    public ResponseEntity<ApiResponse<Pagination<PinResponse.MyPin>>> getMyPinList(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor
    );

    @Operation(
            summary = "특정 장소 노래의 PIN 목록 조회",
            description = "특정 장소 노래의 PIN 목록을 조회합니다. (Figma 기준 화면: PN-01-03)"
    )
    public ResponseEntity<ApiResponse<Pagination<PinResponse.PinDetail>>> getPlaceTrackPinList(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor,
            @PathVariable Long placeTrackId
    );
}
