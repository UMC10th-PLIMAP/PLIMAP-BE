package com.example.plimap.domain.member.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.controller.docs.MemberControllerDocs;
import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.exception.MemberSuccessCode;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Validated
public class MemberController implements MemberControllerDocs {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

    @Operation(
            summary = "닉네임 중복 확인",
            description = "온보딩/프로필 수정 전에 닉네임 사용 가능 여부를 확인합니다."
    )
    @GetMapping("/nickname/check")
    public ApiResponse<MemberResDTO.NicknameCheck> checkNickname(
            @RequestParam
            @NotBlank
            @Size(min = 2, max = 10)
            @Pattern(regexp = "^[가-힣A-Za-z0-9]+$")
            String nickname
    ) {
        boolean available = memberQueryService.isNicknameAvailable(nickname);
        return ApiResponse.success(MemberSuccessCode.NICKNAME_CHECKED, MemberConverter.toNicknameCheck(nickname, available));
    }

    @Override
    @PatchMapping("/me")
    public ApiResponse<MemberResDTO.Profile> updateProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MemberReqDTO.UpdateProfile request
    ) {
        Member member = memberCommandService.updateProfile(authMember.getMember().getId(), request);
        return ApiResponse.success(MemberSuccessCode.PROFILE_UPDATED, MemberConverter.toProfile(member));
    }
}
