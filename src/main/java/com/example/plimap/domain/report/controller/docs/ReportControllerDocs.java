package com.example.plimap.domain.report.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.report.dto.request.ReportRequest;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ReportControllerDocs {

    @Operation(
            summary = "회원 신고",
            description = """
                    로그인한 사용자가 다른 회원을 신고합니다. (Figma 기준 화면: FD-02-01)

                    **신고 카테고리(category)**
                    - PERSONAL_INFORMATION_EXPOSURE: 개인정보노출
                    - OBSCENE_OR_HARMFUL: 음란/유해
                    - ABUSE_OR_HATE_SPEECH: 욕설/혐오 표현
                    - COMMERCIAL_OR_PROMOTIONAL: 상업성/홍보성
                    - OTHER: 기타

                    **상세 내용(detail)**
                    - OTHER: 공백이 아닌 상세 내용 필수
                    - 그 외 카테고리: 반드시 생략하거나 null로 전송
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원 신고 접수 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "REPORT_CREATE_SUCCESS",
                                      "message": "신고가 접수되었습니다.",
                                      "result": null
                                    }
                                    """)
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 또는 자기 자신 신고",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "신고 카테고리 누락", value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_400_VALIDATION_FAILED",
                                              "message": "신고 카테고리를 입력해주세요.",
                                              "result": null
                                            }
                                            """),
                                    @ExampleObject(name = "상세 내용 조건 불일치", value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_400_VALIDATION_FAILED",
                                              "message": "신고 상세 내용이 카테고리 조건에 맞지 않습니다.",
                                              "result": null
                                            }
                                            """),
                                    @ExampleObject(name = "자기 자신 신고", value = """
                                            {
                                              "isSuccess": false,
                                              "code": "REPORT_SELF_NOT_ALLOWED",
                                              "message": "자기 자신은 신고할 수 없습니다.",
                                              "result": null
                                            }
                                            """)
                            }
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_401_UNAUTHORIZED",
                                      "message": "인증이 필요합니다.",
                                      "result": null
                                    }
                                    """)
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "신고 대상 회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "MEMBER_NOT_FOUND",
                                      "message": "존재하지 않는 사용자입니다.",
                                      "result": null
                                    }
                                    """)
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 신고한 회원",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "REPORT_MEMBER_ALREADY_EXISTS",
                                      "message": "이미 신고한 회원입니다.",
                                      "result": null
                                    }
                                    """)
                    ))
    })
    ResponseEntity<ApiResponse<Void>> reportMember(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long memberId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "신고 카테고리와 조건에 맞는 상세 내용",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReportRequest.Create.class),
                            examples = {
                                    @ExampleObject(name = "일반 카테고리", value = """
                                            {
                                              "category": "OBSCENE_OR_HARMFUL"
                                            }
                                            """),
                                    @ExampleObject(name = "기타 카테고리", value = """
                                            {
                                              "category": "OTHER",
                                              "detail": "기타 신고 사유입니다."
                                            }
                                            """)
                            }
                    ))
            @RequestBody @Valid ReportRequest.Create request
    );

    @Operation(
            summary = "PIN 신고",
            description = """
                    로그인한 사용자가 공개 피드인 PIN을 신고합니다. (Figma 기준 화면: PN-01-03-a, b, c, d)

                    **신고 카테고리(category)**
                    - PERSONAL_INFORMATION_EXPOSURE: 개인정보노출
                    - OBSCENE_OR_HARMFUL: 음란/유해
                    - ABUSE_OR_HATE_SPEECH: 욕설/혐오 표현
                    - COMMERCIAL_OR_PROMOTIONAL: 상업성/홍보성
                    - OTHER: 기타

                    **상세 내용(detail)**
                    - OTHER: 공백이 아닌 상세 내용 필수
                    - 그 외 카테고리: 반드시 생략하거나 null로 전송
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "PIN 신고 접수 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "REPORT_CREATE_SUCCESS",
                                      "message": "신고가 접수되었습니다.",
                                      "result": null
                                    }
                                    """)
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 또는 신고할 수 없는 PIN",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "신고 카테고리 누락", value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_400_VALIDATION_FAILED",
                                              "message": "신고 카테고리를 입력해주세요.",
                                              "result": null
                                            }
                                            """),
                                    @ExampleObject(name = "상세 내용 조건 불일치", value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_400_VALIDATION_FAILED",
                                              "message": "신고 상세 내용이 카테고리 조건에 맞지 않습니다.",
                                              "result": null
                                            }
                                            """),
                                    @ExampleObject(name = "자신이 작성한 PIN", value = """
                                            {
                                              "isSuccess": false,
                                              "code": "REPORT_OWN_PIN_NOT_ALLOWED",
                                              "message": "자신이 작성한 PIN은 신고할 수 없습니다.",
                                              "result": null
                                            }
                                            """),
                                    @ExampleObject(name = "비공개 PIN", value = """
                                            {
                                              "isSuccess": false,
                                              "code": "REPORT_PRIVATE_PIN_NOT_ALLOWED",
                                              "message": "공개 피드가 아닌 PIN은 신고할 수 없습니다.",
                                              "result": null
                                            }
                                            """)
                            }
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_401_UNAUTHORIZED",
                                      "message": "인증이 필요합니다.",
                                      "result": null
                                    }
                                    """)
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "신고 대상 PIN 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "PIN_NOT_FOUND",
                                      "message": "핀을 찾을 수 없습니다.",
                                      "result": null
                                    }
                                    """)
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 신고한 PIN",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "REPORT_PIN_ALREADY_EXISTS",
                                      "message": "이미 신고한 PIN입니다.",
                                      "result": null
                                    }
                                    """)
                    ))
    })
    ResponseEntity<ApiResponse<Void>> reportPin(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "신고 카테고리와 조건에 맞는 상세 내용",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReportRequest.Create.class),
                            examples = {
                                    @ExampleObject(name = "일반 카테고리", value = """
                                            {
                                              "category": "ABUSE_OR_HATE_SPEECH"
                                            }
                                            """),
                                    @ExampleObject(name = "기타 카테고리", value = """
                                            {
                                              "category": "OTHER",
                                              "detail": "기타 신고 사유입니다."
                                            }
                                            """)
                            }
                    ))
            @RequestBody @Valid ReportRequest.Create request
    );
}
