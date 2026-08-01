package com.example.plimap.domain.admin.controller;

import com.example.plimap.domain.admin.controller.docs.AdminControllerDocs;
import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.admin.exception.AdminSuccessCode;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController implements AdminControllerDocs {

    @Override
    @GetMapping("/me")
    public ApiResponse<AdminResDTO.Me> getMe(@AuthenticationPrincipal AuthMember authMember) {
        return ApiResponse.success(
                AdminSuccessCode.ME_FETCHED,
                AdminResDTO.Me.from(authMember.getMember())
        );
    }
}
