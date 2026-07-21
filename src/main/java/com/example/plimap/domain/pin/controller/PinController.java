package com.example.plimap.domain.pin.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.pin.controller.docs.PinControllerDocs;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.exception.PinSuccessCode;
import com.example.plimap.domain.pin.service.command.impl.PinCommandServiceImpl;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
