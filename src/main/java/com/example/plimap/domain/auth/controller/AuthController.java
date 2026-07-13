package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.exception.MemberSuccessCode;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberCommandService memberCommandService;

    @Operation(
            summary = "온보딩 (닉네임/프로필 설정)",
            description = "최초 가입 후 닉네임과 프로필 정보를 등록하여 온보딩을 완료합니다."
    )
    @PostMapping("/onboarding")
    public ApiResponse<MemberResDTO.Onboarding> onboarding(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MemberReqDTO.Onboarding request
    ) {
        Member member = memberCommandService.completeOnboarding(authMember.getMember().getId(), request);
        return ApiResponse.success(MemberSuccessCode.ONBOARDING_COMPLETED, MemberConverter.toOnboarding(member));
    }
}
