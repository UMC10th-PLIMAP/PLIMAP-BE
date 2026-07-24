package com.example.plimap.domain.report.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.report.controller.docs.ReportControllerDocs;
import com.example.plimap.domain.report.dto.request.ReportRequest;
import com.example.plimap.domain.report.exception.ReportSuccessCode;
import com.example.plimap.domain.report.service.command.ReportCommandService;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
@Tag(name = "Report", description = "신고 관련 API")
public class ReportController implements ReportControllerDocs {

    private final ReportCommandService reportCommandService;

    @Override
    @PostMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> reportMember(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long memberId,
            @RequestBody @Valid ReportRequest.Create request
    ) {
        reportCommandService.reportMember(currentMember.getMember(), memberId, request);
        return ResponseEntity
                .status(ReportSuccessCode.REPORT_CREATE_SUCCESS.getStatus())
                .body(ApiResponse.success(ReportSuccessCode.REPORT_CREATE_SUCCESS, null));
    }

    @Override
    @PostMapping("/pins/{pinId}")
    public ResponseEntity<ApiResponse<Void>> reportPin(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId,
            @RequestBody @Valid ReportRequest.Create request
    ) {
        reportCommandService.reportPin(currentMember.getMember(), pinId, request);
        return ResponseEntity
                .status(ReportSuccessCode.REPORT_CREATE_SUCCESS.getStatus())
                .body(ApiResponse.success(ReportSuccessCode.REPORT_CREATE_SUCCESS, null));
    }
}