package com.example.plimap.domain.admin.controller;

import com.example.plimap.domain.admin.controller.docs.AdminControllerDocs;
import com.example.plimap.domain.admin.dto.request.AdminReqDTO;
import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.admin.exception.AdminSuccessCode;
import com.example.plimap.domain.admin.service.command.AdminCommandService;
import com.example.plimap.domain.admin.service.query.AdminQueryService;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.pin.enums.PinReportFilter;
import com.example.plimap.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController implements AdminControllerDocs {

    private final AdminCommandService adminCommandService;
    private final AdminQueryService adminQueryService;

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

    @Override
    @GetMapping("/pins/reports")
    public ApiResponse<AdminResDTO.ReportedPinPage> getReportedPins(
            @RequestParam(defaultValue = "ALL") PinReportFilter filter,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(
                AdminSuccessCode.REPORTED_PINS_FETCHED,
                adminQueryService.getReportedPins(filter, page, pageSize)
        );
    }

    @Override
    @GetMapping("/members")
    public ApiResponse<AdminResDTO.MemberPage> getMembers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MemberStatus status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(
                AdminSuccessCode.MEMBERS_FETCHED,
                adminQueryService.getMembers(query, status, page, pageSize)
        );
    }

    @Override
    @GetMapping("/members/{memberId}")
    public ApiResponse<AdminResDTO.MemberDetail> getMemberDetail(@PathVariable Long memberId) {
        return ApiResponse.success(
                AdminSuccessCode.MEMBER_DETAIL_FETCHED,
                adminQueryService.getMemberDetail(memberId)
        );
    }

    @Override
    @PostMapping("/members/{memberId}/nickname/regenerate")
    public ApiResponse<AdminResDTO.MemberDetail> regenerateMemberNickname(@PathVariable Long memberId) {
        return ApiResponse.success(
                AdminSuccessCode.MEMBER_NICKNAME_REGENERATED,
                adminCommandService.regenerateMemberNickname(memberId)
        );
    }

    @Override
    @GetMapping("/inquiries")
    public ApiResponse<AdminResDTO.InquiryPage> getInquiries(
            @RequestParam(required = false) InquiryCategory category,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(
                AdminSuccessCode.INQUIRIES_FETCHED,
                adminQueryService.getInquiries(category, cursor, pageSize)
        );
    }

    @Override
    @GetMapping("/inquiries/{inquiryId}")
    public ApiResponse<AdminResDTO.InquiryDetail> getInquiryDetail(@PathVariable Long inquiryId) {
        return ApiResponse.success(
                AdminSuccessCode.INQUIRY_DETAIL_FETCHED,
                adminQueryService.getInquiryDetail(inquiryId)
        );
    }
}
