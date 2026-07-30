package com.example.plimap.domain.member.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.controller.docs.AdminMemberControllerDocs;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.exception.MemberSuccessCode;
import com.example.plimap.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMemberController implements AdminMemberControllerDocs {

    @Override
    @GetMapping("/me")
    public ApiResponse<MemberResDTO.AdminMe> getAdminMe(@AuthenticationPrincipal AuthMember authMember) {
        return ApiResponse.success(
                MemberSuccessCode.ADMIN_ME_FETCHED,
                MemberResDTO.AdminMe.from(authMember.getMember())
        );
    }
}
