package com.example.plimap.domain.auth.controller.docs;

import com.example.plimap.domain.auth.dto.request.AuthReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import com.example.plimap.global.config.SwaggerConfig;
import com.example.plimap.global.swagger.CommonSwaggerErrorExamples;
import com.example.plimap.global.swagger.ErrorApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "회원 ID 요청 값 검증에 실패한 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "COMMON_400_VALIDATION_FAILED",
                                            summary = "요청 값 검증 실패",
                                            value = CommonSwaggerErrorExamples.VALIDATION_FAILED
                                    ),
                                    @ExampleObject(
                                            name = "COMMON_400_MALFORMED_JSON",
                                            summary = "잘못된 JSON 본문",
                                            value = CommonSwaggerErrorExamples.MALFORMED_JSON
                                    )
                            }
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "테스트 토큰 발급 키가 없거나 올바르지 않은 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "AUTH_TEST_TOKEN_ISSUE_UNAUTHORIZED",
                                    summary = "테스트 토큰 발급 인증 실패",
                                    value = AuthSwaggerErrorExamples.TEST_TOKEN_ISSUE_UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "활성 회원을 찾을 수 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "MEMBER_NOT_FOUND",
                                    summary = "회원 없음",
                                    value = AuthSwaggerErrorExamples.MEMBER_NOT_FOUND
                            )
                    ))
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "테스트 토큰 발급 성공")
    ApiResponse<MemberResponse.Login> issueTestToken(
            @Parameter(hidden = true) String issueKey,
            AuthReqDTO.TempToken request
    );
}
