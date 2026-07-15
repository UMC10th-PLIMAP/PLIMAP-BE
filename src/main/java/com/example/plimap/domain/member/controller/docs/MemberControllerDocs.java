package com.example.plimap.domain.member.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Tag(name = "Member", description = "회원 API")
public interface MemberControllerDocs {

    @Operation(
            summary = "닉네임 중복 확인",
            description = "온보딩/프로필 수정 전에 닉네임 사용 가능 여부를 확인합니다."
    )
    ApiResponse<MemberResDTO.NicknameCheck> checkNickname(
            @NotBlank
            @Size(min = 2, max = 10)
            @Pattern(regexp = "^[가-힣A-Za-z0-9]+$")
            String nickname
    );

    @Operation(
            summary = "내 프로필 수정",
            description = "닉네임, 이름, 소개, 프로필 이미지를 수정합니다. 요청에 포함하지 않은 필드는 변경되지 않습니다."
    )
    ApiResponse<MemberResDTO.Profile> updateProfile(AuthMember authMember, @Valid MemberReqDTO.UpdateProfile request);
}
