package com.example.plimap.domain.member.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Member", description = "회원 API")
public interface MemberControllerDocs {

    @Operation(
            summary = "내 프로필 수정",
            description = "닉네임, 이름, 소개, 프로필 이미지를 수정합니다. 요청에 포함하지 않은 필드는 변경되지 않습니다."
    )
    ApiResponse<MemberResDTO.Profile> updateProfile(AuthMember authMember, @Valid MemberReqDTO.UpdateProfile request);
}
