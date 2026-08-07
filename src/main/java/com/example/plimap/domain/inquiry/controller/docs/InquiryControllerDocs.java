package com.example.plimap.domain.inquiry.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.inquiry.dto.request.InquiryRequest;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

public interface InquiryControllerDocs {

    @Operation(
            summary = "문의 등록",
            description = """
                    로그인 여부와 관계없이 문의를 접수합니다. 로그인 상태라면 작성자(member)가 자동으로 연결되고,
                    비로그인·탈퇴 회원이라면 연결되지 않습니다.

                    **문의 카테고리(category)**
                    - ACCOUNT_SUSPENSION_OR_WITHDRAWAL: 계정 정지/탈퇴 관련
                    - LOGIN_AUTH_ERROR: 로그인/인증 오류
                    - PIN_REGISTRATION_OR_PLAYBACK_ERROR: 핀(PIN) 등록·재생 오류
                    - REPORT_SANCTION_APPEAL: 신고/제재 이의제기
                    - APP_BUG_OR_ERROR: 앱 버그/오류
                    - OTHER: 기타

                    **답변받을 이메일(contactEmail)**
                    - 로그인 사용자: 가입 이메일을 기본값으로 프론트에서 자동 입력하되 수정 가능
                    - 비로그인·탈퇴 사용자: 직접 입력 필수
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "문의 접수 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "INQUIRY_CREATE_SUCCESS",
                                      "message": "문의가 접수되었습니다.",
                                      "result": null
                                    }
                                    """)
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "카테고리 누락", value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_400_VALIDATION_FAILED",
                                              "message": "문의 카테고리를 입력해주세요.",
                                              "result": null
                                            }
                                            """),
                                    @ExampleObject(name = "이메일 형식 오류", value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_400_VALIDATION_FAILED",
                                              "message": "이메일 형식이 올바르지 않습니다.",
                                              "result": null
                                            }
                                            """)
                            }
                    ))
    })
    ResponseEntity<ApiResponse<Void>> createInquiry(
            @AuthenticationPrincipal AuthMember currentMember,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "문의 카테고리, 제목, 내용, 답변받을 이메일",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InquiryRequest.Create.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "category": "APP_BUG_OR_ERROR",
                                      "title": "핀 재생이 안 돼요",
                                      "content": "특정 곡의 PIN이 재생되지 않습니다.",
                                      "contactEmail": "user@example.com"
                                    }
                                    """)
                    ))
            @RequestBody @Valid InquiryRequest.Create request
    );
}
