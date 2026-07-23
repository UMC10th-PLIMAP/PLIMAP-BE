package com.example.plimap.domain.pin.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

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
}
