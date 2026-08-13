package com.example.plimap.domain.auth.controller.docs;

import com.example.plimap.domain.auth.dto.request.AuthReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import com.example.plimap.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth (Test)", description = "local/dev 전용 인증 테스트 API")
public interface AuthTestControllerDocs {

    @Operation(
            summary = "Swagger 테스트용 토큰 발급",
            description = """
                    테스트용 JWT 액세스 토큰을 발급합니다.
                    """
    )
    @SecurityRequirement(name = SwaggerConfig.TEST_TOKEN_ISSUE_KEY_SCHEME)
    ApiResponse<MemberResponse.Login> issueTestToken(
            @Parameter(hidden = true) String issueKey,
            AuthReqDTO.TempToken request
    );
}
