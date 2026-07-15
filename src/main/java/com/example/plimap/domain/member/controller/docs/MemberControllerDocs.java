package com.example.plimap.domain.member.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Member", description = "회원 API")
public interface MemberControllerDocs {

    @Operation(
            summary = "팔로우",
            description = "경로의 memberId에 해당하는 회원을 팔로우합니다. 자기 자신은 팔로우할 수 없고, 이미 팔로우 중이면 실패합니다."
    )
    ApiResponse<Void> follow(AuthMember authMember, Long memberId);
}
