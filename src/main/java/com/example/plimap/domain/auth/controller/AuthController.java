package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.controller.docs.AuthControllerDocs;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.request.TermsReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.dto.response.TermsResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.exception.MemberSuccessCode;
import com.example.plimap.domain.member.exception.TermsSuccessCode;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.command.TermsCommandService;
import com.example.plimap.domain.member.service.query.TermsQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final MemberCommandService memberCommandService;
    private final TermsQueryService termsQueryService;
    private final TermsCommandService termsCommandService;

    @Override
    @PostMapping("/onboarding")
    public ApiResponse<MemberResDTO.Onboarding> onboarding(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MemberReqDTO.Onboarding request
    ) {
        Member member = memberCommandService.completeOnboarding(authMember.getMember().getId(), request);
        return ApiResponse.success(MemberSuccessCode.ONBOARDING_COMPLETED, MemberConverter.toOnboarding(member));
    }

    @Override
    @GetMapping("/terms")
    public ApiResponse<List<TermsResDTO.Item>> getActiveTerms() {
        List<TermsResDTO.Item> result = termsQueryService.getActiveTerms().stream()
                .map(TermsResDTO.Item::from)
                .toList();
        return ApiResponse.success(TermsSuccessCode.ACTIVE_TERMS_RETRIEVED, result);
    }

    @Override
    @PostMapping("/terms")
    public ApiResponse<List<TermsResDTO.Result>> agreeToTerms(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody TermsReqDTO.Agree request
    ) {
        List<TermsResDTO.Result> result = termsCommandService.agreeToTerms(authMember.getMember().getId(), request);
        return ApiResponse.success(TermsSuccessCode.TERMS_AGREED, result);
    }
}
