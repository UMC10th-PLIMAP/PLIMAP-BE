package com.example.plimap.domain.pin.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

public interface PinControllerDocs {

    @Operation(
            summary = "PIN 등록",
            description = "해당 pin, 장소, 노래 정보를을 저장합니다."
    )
    public ApiResponse<PinResponse.Summary> createPin(
            @AuthenticationPrincipal AuthMember currentUser,
            @RequestBody @Valid PinRequest.Create request
    );

    @Operation(
            summary = "지도 선택 위치 검증",
            description = "해당 위치에 핀을 등록할 수 있는지 검증합니다."
    )
    public ApiResponse<PinResponse.PinAvailability> validatePinAvailability(
            @RequestBody @Valid PinRequest.PinAvailability request
    );
}
