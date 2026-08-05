package com.example.plimap.domain.admin.controller;

import com.example.plimap.domain.admin.controller.docs.AdminControllerDocs;
import com.example.plimap.domain.admin.dto.request.AdminReqDTO;
import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.admin.exception.AdminSuccessCode;
import com.example.plimap.domain.admin.service.command.AdminCommandService;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController implements AdminControllerDocs {

    private final AdminCommandService adminCommandService;

    @Override
    @GetMapping("/me")
    public ApiResponse<AdminResDTO.Me> getMe(@AuthenticationPrincipal AuthMember authMember) {
        return ApiResponse.success(
                AdminSuccessCode.ME_FETCHED,
                AdminResDTO.Me.from(authMember.getMember())
        );
    }

    @Override
    @PostMapping("/pins/{pinId}/penalty")
    public ApiResponse<Void> reviewPinReport(
            @PathVariable Long pinId,
            @RequestBody @Valid AdminReqDTO.PenaltyDecision request
    ) {
        adminCommandService.reviewPinReport(pinId, request.grantPenalty());
        return ApiResponse.success(AdminSuccessCode.PIN_PENALTY_REVIEWED, null);
    }

    @Override
    @PostMapping("/members/{memberId}/penalty")
    public ApiResponse<Void> reviewProfileReport(
            @PathVariable Long memberId,
            @RequestBody @Valid AdminReqDTO.PenaltyDecision request
    ) {
        adminCommandService.reviewProfileReport(memberId, request.grantPenalty());
        return ApiResponse.success(AdminSuccessCode.PROFILE_PENALTY_REVIEWED, null);
    }
}
