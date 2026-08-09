package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.controller.docs.AuthTestControllerDocs;
import com.example.plimap.domain.auth.dto.request.AuthReqDTO;
import com.example.plimap.domain.auth.service.command.TestTokenCommandService;
import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.exception.MemberSuccessCode;
import com.example.plimap.global.apiPayload.ApiResponse;
import com.example.plimap.global.config.SwaggerConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile({"local", "dev"})
public class AuthTestController implements AuthTestControllerDocs {

    private final TestTokenCommandService testTokenCommandService;

    @PostMapping("/token/test")
    @Override
    public ApiResponse<MemberResDTO.Login> issueTestToken(
            @RequestHeader(name = SwaggerConfig.TEST_TOKEN_ISSUE_KEY_HEADER, required = false)
            String issueKey,
            @Valid @RequestBody AuthReqDTO.TempToken request
    ) {
        String accessToken = testTokenCommandService.issueTestToken(request.getMemberId(), issueKey);
        return ApiResponse.success(MemberSuccessCode.LOGIN, MemberConverter.toLogin(accessToken));
    }
}
