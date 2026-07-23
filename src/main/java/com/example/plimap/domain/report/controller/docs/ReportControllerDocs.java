package com.example.plimap.domain.report.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.report.dto.request.ReportRequest;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ReportControllerDocs {

    @Operation(
            summary = "회원 신고 접수",
            description = "로그인한 사용자가 다른 회원을 신고합니다."
    )
    ResponseEntity<ApiResponse<Void>> reportMember(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long memberId,
            @RequestBody @Valid ReportRequest.Create request
    );

    @Operation(
            summary = "PIN 신고 접수",
            description = "로그인한 사용자가 공개 피드인 PIN을 신고합니다."
    )
    ResponseEntity<ApiResponse<Void>> reportPin(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId,
            @RequestBody @Valid ReportRequest.Create request
    );
}