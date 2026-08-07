package com.example.plimap.domain.inquiry.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.inquiry.controller.docs.InquiryControllerDocs;
import com.example.plimap.domain.inquiry.dto.request.InquiryRequest;
import com.example.plimap.domain.inquiry.exception.InquirySuccessCode;
import com.example.plimap.domain.inquiry.service.command.InquiryCommandService;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inquiries")
@Tag(name = "Inquiry", description = "문의하기 관련 API")
public class InquiryController implements InquiryControllerDocs {

    private final InquiryCommandService inquiryCommandService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createInquiry(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestBody @Valid InquiryRequest.Create request
    ) {
        inquiryCommandService.createInquiry(resolveMember(currentMember), request);
        return ResponseEntity
                .status(InquirySuccessCode.INQUIRY_CREATE_SUCCESS.getStatus())
                .body(ApiResponse.success(InquirySuccessCode.INQUIRY_CREATE_SUCCESS, null));
    }

    // 비로그인 사용자는 currentMember가 null이고, 탈퇴 회원은 여전히 유효한 토큰으로 인증될 수 있으므로
    // 둘 다 문의 작성자(member)를 null로 남겨 신원을 연결하지 않는다.
    private Member resolveMember(AuthMember currentMember) {
        if (currentMember == null) {
            return null;
        }
        Member member = currentMember.getMember();
        return member.getStatus() == MemberStatus.WITHDRAWN ? null : member;
    }
}
