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
            description = "해당 pin, 장소, 노래 정보를을 저장합니다.(Figma 기준 화면: PN-03-02)"
    )
    public ApiResponse<PinResponse.Summary> createPin(
            @AuthenticationPrincipal AuthMember currentUser,
            @RequestBody @Valid PinRequest.Create request
    );
}
