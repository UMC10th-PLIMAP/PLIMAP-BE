package com.example.plimap.domain.member.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin", description = "관리자 API")
public interface AdminMemberControllerDocs {

    @Operation(
            summary = "관리자 로그인 게이트 확인",
            description = """
                    현재 로그인한 계정이 관리자(ADMIN) 권한을 가지고 있는지 확인합니다.

                    `/api/v1/admin/**` 경로는 SecurityConfig에서 ADMIN 권한이 없으면 403으로 차단되므로,
                    이 API가 200으로 응답하면 관리자 페이지 접근을 허용해도 됩니다.
                    """
    )
    ApiResponse<MemberResDTO.AdminMe> getAdminMe(AuthMember authMember);
}
