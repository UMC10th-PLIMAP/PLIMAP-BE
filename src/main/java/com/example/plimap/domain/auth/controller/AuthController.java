package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.dto.request.AuthReqDTO;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.exception.MemberSuccessCode;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.global.apiPayload.ApiResponse;
import com.example.plimap.global.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile({"local", "dev"})
public class AuthController {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "[임시] Swagger 테스트용 토큰 발급",
            description = "멤버 ID로 JWT 액세스 토큰을 발급합니다. **local/dev 환경에서만 동작합니다.**"
    )
    @SecurityRequirements
    @PostMapping("/token/test")
    public ApiResponse<MemberResDTO.Login> issueTestToken(
            @Valid @RequestBody AuthReqDTO.TempToken request
    ) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        String accessToken = jwtUtil.createAccessToken(new AuthMember(member));
        return ApiResponse.success(MemberSuccessCode.LOGIN, MemberConverter.toLogin(accessToken));
    }
}
